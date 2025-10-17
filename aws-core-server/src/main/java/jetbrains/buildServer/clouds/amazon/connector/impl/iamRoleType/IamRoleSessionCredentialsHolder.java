

package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
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
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_ARN_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_PARAM;

public class IamRoleSessionCredentialsHolder implements AwsCredentialsHolder {

  private final SProjectFeatureDescriptor myIamRoleConnectionFeature;
  private final LinkedAwsConnectionProvider myLinkedConnectionProvider;
  private final StsClientProvider myStsClientProvider;
  private final AwsExternalIdsManager myAwsExternalIdsManager;

  public IamRoleSessionCredentialsHolder(@NotNull final SProjectFeatureDescriptor iamRoleConnectionFeature,
                                         @NotNull final LinkedAwsConnectionProvider linkedConnectionProvider,
                                         @NotNull final StsClientProvider stsClientProvider,
                                         @NotNull final AwsExternalIdsManager awsExternalIdsManager) {
    myIamRoleConnectionFeature = iamRoleConnectionFeature;
    myLinkedConnectionProvider = linkedConnectionProvider;
    myStsClientProvider = stsClientProvider;
    myAwsExternalIdsManager = awsExternalIdsManager;
  }

  @Nullable
  private String getAwsConnectionExternalId() {
    String externalId = null;
    try {
      externalId = myAwsExternalIdsManager.getAwsConnectionExternalId(
        myIamRoleConnectionFeature.getId(),
        myIamRoleConnectionFeature.getProjectId()
      );
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format(
          "Failed to get the External ID to assume the IAM Role with ARN <%s>, Connection: %s in Project: %s, reason: %s",
          myIamRoleConnectionFeature.getParameters().get(IAM_ROLE_ARN_PARAM),
          myIamRoleConnectionFeature.getId(),
          myIamRoleConnectionFeature.getProjectId(),
          e.getMessage()
        ), e);
    }
    return externalId;
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() throws ConnectionCredentialsException {
    Credentials credentials = assumeIamRole().credentials();
    return AwsConnectionUtils.getDataFromCredentials(credentials);
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

  private AssumeRoleResponse assumeIamRole() throws ConnectionCredentialsException {
    StsClient sts = myStsClientProvider
      .getClientWithCredentials(
        new AwsConnectionCredentials(
          myLinkedConnectionProvider.getLinkedConnectionCredentials(myIamRoleConnectionFeature)
        ),
        myIamRoleConnectionFeature.getParameters()
      );

    Map<String, String> connectionProperties = myIamRoleConnectionFeature.getParameters();
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

    return IOGuard.allowNetworkCall(() -> sts.assumeRole(assumeRoleRequest.build()));
  }
}