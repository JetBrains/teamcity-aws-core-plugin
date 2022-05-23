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

package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.apache.http.ParseException;
import org.jetbrains.annotations.NotNull;

public class AwsKeyRotatorImpl implements AwsKeyRotator {

  private static final long waitUntilRotatedKeyIsAvailableTimeOutSec = 30;
  private final OAuthConnectionsManager myOAuthConnectionsManager;

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
  }

  @Override
  public void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws AwsConnectorException {
    OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (awsConnectionDescriptor == null) {
      return;
    }

    AWSCredentialsProvider currentCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );

    AmazonIdentityManagement iam = createIamClient(currentCredentials);
    AWSSecurityTokenService sts = StsClientBuilder.buildStsClientWithCredentials(awsConnectionDescriptor.getParameters(), currentCredentials);

    String iamUserName = getIamUserName(iam);
    CreateAccessKeyResult createAccessKeyResult = createAccessKey(iam, iamUserName);

    AWSCredentialsProvider newCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        createAccessKeyResult.getAccessKey().getAccessKeyId(),
        createAccessKeyResult.getAccessKey().getSecretAccessKey()
      )
    );

    waitUntilRotatedKeyIsAvailable(awsConnectionDescriptor.getParameters(), newCredentials);
    updateConnection(awsConnectionDescriptor.getId(), newCredentials, project);
    waitUntilRotatedKeyIsSaved(awsConnectionDescriptor.getId(), newCredentials, project);
    deleteAccessKey(iam, iamUserName, currentCredentials);
  }

  @NotNull
  private String getIamUserName(@NotNull final AmazonIdentityManagement iam) {
    return iam.getUser().getUser().getUserName();
  }

  @NotNull
  private AmazonIdentityManagement createIamClient(@NotNull final AWSCredentialsProvider creds) {
    return AmazonIdentityManagementClientBuilder
      .standard()
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"))
      .withCredentials(creds)
      .build();
  }

  @NotNull
  private CreateAccessKeyResult createAccessKey(@NotNull final AmazonIdentityManagement iam, @NotNull final String iamUserName) {
    CreateAccessKeyRequest createAccessKeyRequest = new CreateAccessKeyRequest()
      .withUserName(iamUserName);
    return iam.createAccessKey(createAccessKeyRequest);
  }

  private void waitUntilRotatedKeyIsAvailable(@NotNull final Map<String, String> connectionProperties, @NotNull final AWSCredentialsProvider credentials)
    throws AwsConnectorException {
    AWSSecurityTokenService sts = StsClientBuilder.buildStsClientWithCredentials(connectionProperties, credentials);

    long elapsedTimeSec = 0;
    int waitPerTrySec = 3;
    Exception rotatedConnectionException = new Exception();

    while (elapsedTimeSec <= waitUntilRotatedKeyIsAvailableTimeOutSec) {
      try {
        Thread.sleep(waitPerTrySec * 1000);
        sts.getCallerIdentity(new GetCallerIdentityRequest());
        return;
      } catch (Exception e) {
        rotatedConnectionException = e;
        elapsedTimeSec += waitPerTrySec;
      }
    }

    throw new AwsConnectorException("Rotated connection is invalid: " + rotatedConnectionException.getMessage());
  }

  private void updateConnection(@NotNull final String connectionId, @NotNull final AWSCredentialsProvider newCredentials, @NotNull SProject project) throws AwsConnectorException {
    OAuthConnectionDescriptor currentConnection = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (currentConnection == null) {
      throw new AwsConnectorException("The connection has been deleted while it was being rotated. Cannot update connection with ID: " + connectionId);
    }

    Map<String, String> newParameters = new HashMap<>(currentConnection.getParameters());
    newParameters.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, newCredentials.getCredentials().getAWSAccessKeyId());
    newParameters.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, newCredentials.getCredentials().getAWSSecretKey());

    myOAuthConnectionsManager.updateConnection(project, currentConnection.getId(), currentConnection.getOauthProvider().getType(), newParameters);
    project.schedulePersisting("Connection updated");
  }

  private void waitUntilRotatedKeyIsSaved(@NotNull final String connectionId, @NotNull final AWSCredentialsProvider credentials, @NotNull final SProject project)
    throws AwsConnectorException {
    OAuthConnectionDescriptor awsConnectionDescriptor;

    long elapsedTimeSec = 0;
    int waitPerTrySec = 3;
    Exception rotatedConnectionException = new Exception();

    while (elapsedTimeSec <= waitUntilRotatedKeyIsAvailableTimeOutSec) {
      try {
        Thread.sleep(waitPerTrySec * 1000);
        awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
        assert awsConnectionDescriptor != null;
        String currentKeyInSavedConnection = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
        String newAccessKeyId = credentials.getCredentials().getAWSAccessKeyId();
        if (!currentKeyInSavedConnection.equals(newAccessKeyId)) {
          throw new IllegalStateException(
            "The rotated key has not been updated in the project: " + project.getExternalId() + ", connection ID: " + awsConnectionDescriptor.getId());
        }
        return;
      } catch (Exception e) {
        rotatedConnectionException = e;
        elapsedTimeSec += waitPerTrySec;
      }
    }

    throw new AwsConnectorException("Rotated connection is invalid after " + waitUntilRotatedKeyIsAvailableTimeOutSec + " seconds: " + rotatedConnectionException.getMessage());
  }

  private void deleteAccessKey(@NotNull final AmazonIdentityManagement iam, @NotNull final String iamUserName, @NotNull final AWSCredentialsProvider currentCredentials) {
    DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
      .withUserName(iamUserName)
      .withAccessKeyId(currentCredentials.getCredentials().getAWSAccessKeyId());
    iam.deleteAccessKey(deleteAccessKeyRequest);
  }
}