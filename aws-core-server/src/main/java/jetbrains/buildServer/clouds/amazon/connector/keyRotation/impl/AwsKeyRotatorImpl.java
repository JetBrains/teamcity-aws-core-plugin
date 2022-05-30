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
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.amazon.retry.impl.DelayListener;
import jetbrains.buildServer.util.amazon.retry.impl.RetrierImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AwsKeyRotatorImpl implements AwsKeyRotator {

  private static int ROTATE_TIMEOUT_SEC = 30;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final EventDispatcher<BuildServerListener> myBuildServerEventDispatcher;
  private final AmazonIdentityManagement myIam;
  private final AWSSecurityTokenService mySts;

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myBuildServerEventDispatcher = buildServerEventDispatcher;

    myIam = AmazonIdentityManagementClientBuilder
      .standard()
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"))
      .build();

    mySts = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"))
      .build();
  }

  public void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws KeyRotationException {
    OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (awsConnectionDescriptor == null) {
      throw new KeyRotationException("The AWS Connection with ID " + connectionId + " was not found.");
    }

    Loggers.CLOUD.info("Key rotation initiated for the AWS key: " + ParamUtil.maskKey(awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM)));
    AWSCredentialsProvider previousCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );

    Loggers.CLOUD.debug("Creating a new key...");
    CreateAccessKeyResult createAccessKeyResult = createAccessKey(previousCredentials);
    AWSCredentialsProvider newCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        createAccessKeyResult.getAccessKey().getAccessKeyId(),
        createAccessKeyResult.getAccessKey().getSecretAccessKey()
      )
    );

    Loggers.CLOUD.debug("Waiting for the new key to become available...");
    waitUntilRotatedKeyIsAvailable(newCredentials);

    Loggers.CLOUD.debug("Updating the AWS Connection...");
    updateConnection(awsConnectionDescriptor.getId(), newCredentials, project);

    myBuildServerEventDispatcher.addListener(new BuildServerAdapter() {
      private final EventDispatcher<BuildServerListener> myEventDispatcher = myBuildServerEventDispatcher;
      private final String myPreviousAccessKeyId = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
      private final String myNewAccessKeyId = createAccessKeyResult.getAccessKey().getSecretAccessKey();
      private final String myProjectFeatureId = awsConnectionDescriptor.getId();
      private final String myProjectId = project.getExternalId();

      @Override
      public void projectFeatureChanged(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor before, @NotNull final SProjectFeatureDescriptor after) {

        if (!myProjectId.equals(project.getExternalId()) || !myProjectFeatureId.equals(after.getId())) {
          return;
        }

        String previousAccessKeyId = before.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
        String newAccessKeyId = after.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
        if (myPreviousAccessKeyId.equals(previousAccessKeyId) && myNewAccessKeyId.equals(newAccessKeyId)) {
          try {
            Loggers.CLOUD.info("Deleting the previous key " + ParamUtil.maskKey(previousAccessKeyId));
            deleteAccessKey(previousAccessKeyId);
          } catch (KeyRotationException keyRotationException) {
            Loggers.CLOUD.warn("Failed to delete old AWS key after rotation: " + keyRotationException.getMessage());
          }
        } else {
          Loggers.CLOUD.warn("Something tried to change the key " + ParamUtil.maskKey(previousAccessKeyId) + " when it is being rotated!");
        }

        myEventDispatcher.removeListener(this);
      }
    });
  }

  @NotNull
  private String getIamUserName(@NotNull final AWSCredentialsProvider creds) {
    GetUserRequest getUserRequest = new GetUserRequest()
      .withRequestCredentialsProvider(creds);
    return myIam.getUser(getUserRequest).getUser().getUserName();
  }

  @NotNull
  private CreateAccessKeyResult createAccessKey(@NotNull final AWSCredentialsProvider previousCredentials)
    throws KeyRotationException {
    String iamUserName = getIamUserName(previousCredentials);
    CreateAccessKeyRequest createAccessKeyRequest = new CreateAccessKeyRequest()
      .withUserName(iamUserName)
      .withRequestCredentialsProvider(previousCredentials);
    try {
      return myIam.createAccessKey(createAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  private void waitUntilRotatedKeyIsAvailable(@NotNull final AWSCredentialsProvider credentials)
    throws KeyRotationException {
    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest()
      .withRequestCredentialsProvider(credentials);
    try {
      new RetrierImpl(ROTATE_TIMEOUT_SEC)
        .registerListener(new DelayListener(1000))
        .execute(() -> mySts.getCallerIdentity(getCallerIdentityRequest));

    } catch (RuntimeException e) {
      throw new KeyRotationException("Rotated connection is invalid after " + ROTATE_TIMEOUT_SEC + " seconds: " + e.getMessage(), e);
    }
  }

  private void updateConnection(@NotNull final String connectionId,
                                @NotNull final AWSCredentialsProvider newCredentials,
                                @NotNull SProject project)
    throws KeyRotationException {
    OAuthConnectionDescriptor currentConnection = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (currentConnection == null) {
      throw new KeyRotationException("The connection has been deleted while it was being rotated. Cannot update connection with ID: " + connectionId);
    }

    Map<String, String> newParameters = new HashMap<>(currentConnection.getParameters());
    newParameters.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, newCredentials.getCredentials().getAWSAccessKeyId());
    newParameters.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, newCredentials.getCredentials().getAWSSecretKey());

    myOAuthConnectionsManager.updateConnection(project, currentConnection.getId(), currentConnection.getOauthProvider().getType(), newParameters);
    project.schedulePersisting("Connection updated");
  }

  private void deleteAccessKey(@NotNull final String accessKeyId)
    throws KeyRotationException {
    DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
      .withAccessKeyId(accessKeyId);
    try {
      myIam.deleteAccessKey(deleteAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  @Used("tests")
  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                       @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher,
                       @NotNull final AmazonIdentityManagement iam,
                       @NotNull final AWSSecurityTokenService sts) {
    ROTATE_TIMEOUT_SEC = 1;
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myBuildServerEventDispatcher = buildServerEventDispatcher;
    myIam = iam;
    mySts = sts;
  }
}