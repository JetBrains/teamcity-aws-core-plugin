

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
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_ARN_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_PARAM;

public class IamRoleSessionCredentialsHolder implements AwsCredentialsHolder {

  private final SProjectFeatureDescriptor myAwsConnectionFeature;
  private final LinkedAwsConnectionProvider myLinkedConnectionProvider;
  private final StsClientProvider myStsClientProvider;
  private final AwsExternalIdsManager myAwsExternalIdsManager;
  private final AwsCredentialsHolderCache myCache;

  public IamRoleSessionCredentialsHolder(@NotNull final SProjectFeatureDescriptor iamRoleConnectionFeature,
                                         @NotNull final LinkedAwsConnectionProvider linkedConnectionProvider,
                                         @NotNull final StsClientProvider stsClientProvider,
                                         @NotNull final AwsExternalIdsManager awsExternalIdsManager,
                                         @NotNull AwsCredentialsHolderCache cache) {
    myAwsConnectionFeature = iamRoleConnectionFeature;
    myLinkedConnectionProvider = linkedConnectionProvider;
    myStsClientProvider = stsClientProvider;
    myAwsExternalIdsManager = awsExternalIdsManager;
    myCache = cache;
  }

  @Nullable
  private String getAwsConnectionExternalId() {
    String externalId = null;
    try {
      externalId = myAwsExternalIdsManager.getAwsConnectionExternalId(
        myAwsConnectionFeature.getId(),
        myAwsConnectionFeature.getProjectId()
      );
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
    StsClient sts = myStsClientProvider
      .getClientWithCredentials(
        new AwsConnectionCredentials(
          myLinkedConnectionProvider.getLinkedConnectionCredentials(myAwsConnectionFeature)
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