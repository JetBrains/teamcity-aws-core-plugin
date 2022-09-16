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

import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.CredentialsRefresher;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_ARN_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_PARAM;

public class IamRoleSessionCredentialsHolder extends CredentialsRefresher {

  private final AssumeRoleRequest myAssumeRoleRequest;

  private volatile AssumeRoleResult currentSession;

  public IamRoleSessionCredentialsHolder(@NotNull final SProjectFeatureDescriptor iamRoleConnectionFeature,
                                         @NotNull final AwsConnectionDescriptor principalAwsConnection,
                                         @NotNull final ExecutorServices executorServices,
                                         @NotNull final AwsExternalIdsManager awsExternalIdsManager) {
    super(principalAwsConnection.getAwsCredentialsHolder(), principalAwsConnection.getParameters(), executorServices);


    Map<String, String> connectionProperties = iamRoleConnectionFeature.getParameters();

    String externalId = null;
    try {
      externalId = awsExternalIdsManager.getAwsConnectionExternalId(iamRoleConnectionFeature);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.debug(
        String.format("Failed to get the External ID to assume the IAM Role with ARN <%s>, reason: %s", connectionProperties.get(IAM_ROLE_ARN_PARAM), e.getMessage()), e
      );
    }

    int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
    myAssumeRoleRequest = new AssumeRoleRequest()
      .withRoleArn(connectionProperties.get(IAM_ROLE_ARN_PARAM))
      .withRoleSessionName(connectionProperties.get(IAM_ROLE_SESSION_NAME_PARAM))
      .withDurationSeconds(sessionDurationMinutes * 60);
    if (externalId != null) {
      myAssumeRoleRequest.setExternalId(externalId);
    }

    currentSession = mySts.assumeRole(myAssumeRoleRequest);
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    Credentials credentials = currentSession.getCredentials();
    return getDataFromCredentials(credentials);
  }

  @Override
  public void refreshCredentials() {
    Loggers.CLOUD.debug("Refreshing AWS Credentials with assume role request...");
    try {
      currentSession = mySts.assumeRole(myAssumeRoleRequest);
    } catch (Exception e) {
      Loggers.CLOUD.warnAndDebugDetails("Failed to refresh AWS Credentials with assume role request: ", e);
    }
  }

  @Override
  @NotNull
  public Date getSessionExpirationDate() {
    return currentSession.getCredentials().getExpiration();
  }
}
