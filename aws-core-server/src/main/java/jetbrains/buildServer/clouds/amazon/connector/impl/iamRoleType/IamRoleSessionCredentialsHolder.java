/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
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
    Credentials credentials = assumeIamRole().getCredentials();
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

  private AssumeRoleResult assumeIamRole() throws ConnectionCredentialsException {
    AWSSecurityTokenService sts = myStsClientProvider
      .getClientWithCredentials(
        new AwsConnectionCredentials(
          myLinkedConnectionProvider.getLinkedConnectionCredentials(myIamRoleConnectionFeature)
        ),
        myIamRoleConnectionFeature.getParameters()
      );

    Map<String, String> connectionProperties = myIamRoleConnectionFeature.getParameters();
    AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
      .withRoleArn(connectionProperties.get(IAM_ROLE_ARN_PARAM))
      .withRoleSessionName(connectionProperties.get(IAM_ROLE_SESSION_NAME_PARAM));

    String sessionDurationParam = connectionProperties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM);
    if (sessionDurationParam != null) {
      int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
      assumeRoleRequest.withDurationSeconds(sessionDurationMinutes * 60);
    }

    String externalId = getAwsConnectionExternalId();
    if (externalId != null) {
      assumeRoleRequest.setExternalId(externalId);
    }

    return IOGuard.allowNetworkCall(() -> sts.assumeRole(assumeRoleRequest));
  }
}
