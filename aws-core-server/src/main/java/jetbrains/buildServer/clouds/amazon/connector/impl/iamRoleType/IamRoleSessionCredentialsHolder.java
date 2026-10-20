

package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsCredentialsHolderCache;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_ARN_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_PARAM;

public class IamRoleSessionCredentialsHolder implements AwsCredentialsHolder {

  public static final String IAM_ROLE_ALLOW_ACCESS_TO_PARENT_PROJECTS = "teamcity.internal.aws.connection.iamRole.allowAccessToParentProjects";
  private final SProjectFeatureDescriptor myAwsConnectionFeature;
  private final LinkedAwsConnectionProvider myLinkedConnectionProvider;
  private final StsClientProvider myStsClientProvider;
  private final AwsExternalIdsManager myAwsExternalIdsManager;
  private final AwsCredentialsHolderCache myCache;
  @NotNull private final SecurityContextEx mySecurityContext;


  public IamRoleSessionCredentialsHolder(@NotNull final SProjectFeatureDescriptor iamRoleConnectionFeature,
                                         @NotNull final LinkedAwsConnectionProvider linkedConnectionProvider,
                                         @NotNull final StsClientProvider stsClientProvider,
                                         @NotNull final AwsExternalIdsManager awsExternalIdsManager,
                                         @NotNull AwsCredentialsHolderCache cache,
                                         @NotNull SecurityContextEx securityContext) {
    myAwsConnectionFeature = iamRoleConnectionFeature;
    myLinkedConnectionProvider = linkedConnectionProvider;
    myStsClientProvider = stsClientProvider;
    myAwsExternalIdsManager = awsExternalIdsManager;
    myCache = cache;
    mySecurityContext = securityContext;
  }

  @Nullable
  private String getAwsConnectionExternalId() {
    String externalId = null;
    try {
      if (TeamCityProperties.getBooleanOrTrue(IAM_ROLE_ALLOW_ACCESS_TO_PARENT_PROJECTS)) {
        externalId = mySecurityContext.runAsSystemUnchecked(
          () -> myAwsExternalIdsManager.getAwsConnectionExternalId(
            myAwsConnectionFeature.getId(),
            myAwsConnectionFeature.getProjectId()
          ));
      } else {
        externalId = myAwsExternalIdsManager.getAwsConnectionExternalId(
          myAwsConnectionFeature.getId(),
          myAwsConnectionFeature.getProjectId()
        );
      }

    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format(
          "Failed to get the External ID to assume the IAM Role with ARN <%s>, Connection: %s in Project: %s, reason: %s",
          myAwsConnectionFeature.getParameters().get(IAM_ROLE_ARN_PARAM),
          myAwsConnectionFeature.getId(),
          myAwsConnectionFeature.getProjectId(),
          e.getMessage()
        ), e);
    }
    return externalId;
  }


  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() throws ConnectionCredentialsException {
    return myCache.getAwsCredentials(myAwsConnectionFeature, this::requestSession);
  }

  @Override
  public void refreshCredentials() {
    //TODO: TW-78235 refactor other parts of AWS Core plugin not to use refreshing logic
  }

  @Override
  @Nullable
  public Date getSessionExpirationDate() {
    //TODO: TW-78235 refactor other parts of AWS Core plugin not to use refreshing logic
    return null;
  }


  private Credentials requestSession() throws ConnectionCredentialsException {
    // TW-98038 Credentials must be requested unchecked because we need to access the project of the connection
    final ConnectionCredentials connectionCredentials;
    if (TeamCityProperties.getBooleanOrTrue(IAM_ROLE_ALLOW_ACCESS_TO_PARENT_PROJECTS)) {
      connectionCredentials = mySecurityContext.runAsSystemUnchecked(
        () -> myLinkedConnectionProvider.getLinkedConnectionCredentials(myAwsConnectionFeature));
    } else {
      connectionCredentials = myLinkedConnectionProvider.getLinkedConnectionCredentials(myAwsConnectionFeature);
    }
    StsClient sts = myStsClientProvider
      .getClientWithCredentials(
        new AwsConnectionCredentials(
          connectionCredentials
        ),
        myAwsConnectionFeature.getParameters()
      );

    Map<String, String> connectionProperties = myAwsConnectionFeature.getParameters();
    AssumeRoleRequest.Builder assumeRoleRequest = AssumeRoleRequest.builder()
                                                                   .roleArn(connectionProperties.get(IAM_ROLE_ARN_PARAM))
                                                                   .roleSessionName(connectionProperties.get(IAM_ROLE_SESSION_NAME_PARAM));

    String sessionDurationParam = connectionProperties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM);
    if (sessionDurationParam != null) {
      int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
      assumeRoleRequest.durationSeconds(sessionDurationMinutes * 60);
    }

    String externalId = getAwsConnectionExternalId();
    if (externalId != null) {
      assumeRoleRequest.externalId(externalId);
    }

    return IOGuard.allowNetworkCall(() -> sts.assumeRole(assumeRoleRequest.build())).credentials();
  }
}