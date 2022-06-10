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
import com.amazonaws.regions.Regions;
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
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.amazon.retry.impl.DelayListener;
import jetbrains.buildServer.util.amazon.retry.impl.RetrierImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AwsKeyRotatorImpl extends BuildServerAdapter implements AwsKeyRotator {

  private static int ROTATE_TIMEOUT_SEC = 30;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;

  private AmazonIdentityManagement myIam;
  private final AmazonIdentityManagementClientBuilder myIamClientBuilder;
  private AWSSecurityTokenService mySts;
  private final AWSSecurityTokenServiceClientBuilder myStsClientBuilder;

  private final ConcurrentLinkedQueue<String> scheduledForDeletionKeys = new ConcurrentLinkedQueue<>();

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher,
                           @NotNull final SecurityContextEx securityContext,
                           @NotNull final ConfigActionFactory configActionFactory) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    buildServerEventDispatcher.addListener(this);

    myIamClientBuilder = AmazonIdentityManagementClientBuilder
      .standard()
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"));

    myStsClientBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }

  public void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws KeyRotationException {
    OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (awsConnectionDescriptor == null) {
      throw new KeyRotationException("The AWS Connection with ID " + connectionId + " was not found.");
    }

    String connectionRegion = awsConnectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM);
    myIam = myIamClientBuilder
      .withRegion(Regions.fromName(connectionRegion))
      .build();
    mySts = myStsClientBuilder
      .withRegion(Regions.fromName(connectionRegion))
      .build();

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
    scheduledForDeletionKeys.add(previousCredentials.getCredentials().getAWSAccessKeyId());
    updateConnection(awsConnectionDescriptor.getId(), newCredentials, project);
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
    persist(project);
  }

  private void persist(@NotNull final SProject project) {
    final AuthorityHolder authHolder = mySecurityContext.getAuthorityHolder();
    if (authHolder instanceof SUser) {
      project.schedulePersisting(myConfigActionFactory.createAction((SUser) authHolder, project, "Connection updated"));
    } else {
      project.schedulePersisting(myConfigActionFactory.createAction(project, "Connection updated"));
    }
  }

  private void deleteAccessKey(@NotNull final String accessKeyId, @NotNull final AWSCredentialsProvider credentials)
    throws KeyRotationException {
    DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
      .withAccessKeyId(accessKeyId)
      .withRequestCredentialsProvider(credentials);
    try {
      myIam.deleteAccessKey(deleteAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  @Override
  public void projectFeatureChanged(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor before, @NotNull final SProjectFeatureDescriptor after) {
    if (!OAuthConstants.FEATURE_TYPE.equals(after.getType())) {
      return;
    }

    String previousAccessKeyId = before.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);

    if (previousAccessKeyId == null || ! scheduledForDeletionKeys.contains(previousAccessKeyId)) {
      return;
    }
    String newAccessKeyId = after.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
    String newSecretKey = after.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM);

    if (! newAccessKeyId.equals(previousAccessKeyId)) {
      try {
        Loggers.CLOUD.info("Deleting the previous key " + ParamUtil.maskKey(previousAccessKeyId));
        AWSCredentialsProvider newCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(newAccessKeyId, newSecretKey));
        deleteAccessKey(previousAccessKeyId, newCredentials);

      } catch (KeyRotationException keyRotationException) {
        Loggers.CLOUD.warn("Failed to delete old AWS key after rotation: " + keyRotationException.getMessage());
      }

      scheduledForDeletionKeys.remove(previousAccessKeyId);
    }
  }

  @Used("tests")
  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher,
                           @NotNull final SecurityContextEx securityContext,
                           @NotNull final ConfigActionFactory configActionFactory,
                           @NotNull final AmazonIdentityManagementClientBuilder iamBuilder,
                           @NotNull final AWSSecurityTokenServiceClientBuilder stsBuilder) {
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    ROTATE_TIMEOUT_SEC = 1;
    myOAuthConnectionsManager = oAuthConnectionsManager;
    buildServerEventDispatcher.addListener(this);
    myIamClientBuilder = iamBuilder;
    myStsClientBuilder = stsBuilder;
  }
}