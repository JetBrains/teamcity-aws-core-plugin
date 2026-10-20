

package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.RotateKeyApi;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.retry.Retrier;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

public class AwsRotateKeyApi implements RotateKeyApi {

  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;
  private final OAuthConnectionDescriptor myAwsConnectionDescriptor;
  private final SProject myProject;
  private final int myRotateTimeoutSec;
  private final IamClient myIam;
  private final StsClient mySts;
  private final AwsCredentialsProvider myPreviousCredentials;
  private AwsCredentialsProvider myNewCredentials;

  public AwsRotateKeyApi(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                         @NotNull final SecurityContextEx securityContext,
                         @NotNull final ConfigActionFactory configActionFactory,
                         @NotNull final OAuthConnectionDescriptor awsConnectionDescriptor,
                         @NotNull final SProject project,
                         final int rotateTimeoutSec) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    myAwsConnectionDescriptor = awsConnectionDescriptor;
    myProject = project;
    myRotateTimeoutSec = rotateTimeoutSec;

    String connectionRegion = awsConnectionDescriptor.getParameters()
      .get(AwsCloudConnectorConstants.REGION_NAME_PARAM);
    Region region = Region.of(connectionRegion);

    myIam = IamClient.builder()
      .region(region)
      .defaultsMode(DefaultsMode.STANDARD)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("iam"))
      .overrideConfiguration(
        ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
          .build())
      .build();

    mySts = StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .region(region)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("sts"))
      .overrideConfiguration(
        ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
          .build()
      )
      .build();

    myPreviousCredentials = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );
  }

  @Override
  public void rotateKey() throws KeyRotationException {
    Loggers.CLOUD.debug("Creating a new key...");
    IOGuard.allowNetworkCall(() -> createNewKey());

    Loggers.CLOUD.debug("Waiting for the new key to become available...");
    IOGuard.allowNetworkCall(() -> waitUntilRotatedKeyIsAvailable());

    Loggers.CLOUD.debug("Updating the AWS Connection...");
    IOGuard.allowNetworkCall(() -> updateConnection());
  }

  private void createNewKey() throws KeyRotationException {
    CreateAccessKeyResponse createAccessKeyResult = createAccessKey(myPreviousCredentials);
    myNewCredentials = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(
        createAccessKeyResult.accessKey().accessKeyId(),
        createAccessKeyResult.accessKey().secretAccessKey()
      )
    );
  }

  private void waitUntilRotatedKeyIsAvailable() throws KeyRotationException {
    GetCallerIdentityRequest getCallerIdentityRequest = GetCallerIdentityRequest.builder()
      .overrideConfiguration(c -> c.credentialsProvider(myNewCredentials))
      .build();

    try {
      Retrier.withRetries(myRotateTimeoutSec, Retrier.DelayStrategy.constantBackOff(1000))
        .execute(() -> mySts.getCallerIdentity(getCallerIdentityRequest));

    } catch (RuntimeException e) {
      throw new KeyRotationException("Rotated connection is invalid after " + myRotateTimeoutSec + " seconds: " + e.getMessage(), e);
    }
  }

  private void updateConnection() throws KeyRotationException {
    OAuthConnectionDescriptor currentConnection = myOAuthConnectionsManager.findConnectionById(myProject, myAwsConnectionDescriptor.getId());
    if (currentConnection == null) {
      throw new KeyRotationException("The connection has been deleted while it was being rotated. Cannot update connection with ID: " + myAwsConnectionDescriptor.getId());
    }

    Map<String, String> newParameters = new HashMap<>(currentConnection.getParameters());
    AwsCredentials awsCredentials = myNewCredentials.resolveCredentials();
    newParameters.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, awsCredentials.accessKeyId());
    newParameters.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, awsCredentials.secretAccessKey());

    myOAuthConnectionsManager.updateConnection(myProject, currentConnection.getId(), currentConnection.getOauthProvider().getType(), newParameters);
    persist(myProject);
  }

  private void persist(@NotNull final SProject project) {
    final AuthorityHolder authHolder = mySecurityContext.getAuthorityHolder();
    if (authHolder instanceof SUser) {
      project.schedulePersisting(myConfigActionFactory.createAction((SUser) authHolder, project, "Connection updated"));
    } else {
      project.schedulePersisting(myConfigActionFactory.createAction(project, "Connection updated"));
    }
  }

  @NotNull
  private CreateAccessKeyResponse createAccessKey(@NotNull final AwsCredentialsProvider previousCredentials)
    throws KeyRotationException {
    String iamUserName = getIamUserName(previousCredentials);
    CreateAccessKeyRequest createAccessKeyRequest = CreateAccessKeyRequest.builder()
      .userName(iamUserName)
      .overrideConfiguration(c -> c.credentialsProvider(previousCredentials))
      .build();

    try {
      return myIam.createAccessKey(createAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  @NotNull
  private String getIamUserName(@NotNull final AwsCredentialsProvider creds) {
    GetUserRequest getUserRequest = GetUserRequest.builder()
      .overrideConfiguration(c -> c.credentialsProvider(creds))
      .build();

    return myIam.getUser(getUserRequest)
      .user()
      .userName();
  }


  @Used("tests")
  public AwsRotateKeyApi(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                         @NotNull final SecurityContextEx securityContext,
                         @NotNull final ConfigActionFactory configActionFactory,
                         @NotNull final OAuthConnectionDescriptor awsConnectionDescriptor,
                         @NotNull final IamClient iam,
                         @NotNull final StsClient sts,
                         @NotNull final SProject project) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    myAwsConnectionDescriptor = awsConnectionDescriptor;
    myIam = iam;
    mySts = sts;
    myProject = project;
    myRotateTimeoutSec = 1;

    myPreviousCredentials = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );
  }
}