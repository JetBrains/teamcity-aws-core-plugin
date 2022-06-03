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
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.amazon.retry.impl.DelayListener;
import jetbrains.buildServer.util.amazon.retry.impl.RetrierImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AwsRotateKeyActions {

  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;
  private final OAuthConnectionDescriptor myAwsConnectionDescriptor;
  private final SProject myProject;
  private final int myRotateTimeoutSec;
  private final AmazonIdentityManagement myIam;
  private final AWSSecurityTokenService mySts;
  private final AWSCredentialsProvider myPreviousCredentials;
  private AWSCredentialsProvider myNewCredentials;

  public AwsRotateKeyActions(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
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

    String connectionRegion = awsConnectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM);

    myIam = AmazonIdentityManagementClientBuilder
      .standard()
      .withRegion(Regions.fromName(connectionRegion))
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"))
      .build();

    mySts = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withRegion(Regions.fromName(connectionRegion))
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"))
      .build();

    myPreviousCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );
  }

  public void createNewKey() throws KeyRotationException {
    CreateAccessKeyResult createAccessKeyResult = createAccessKey(myPreviousCredentials);
    myNewCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        createAccessKeyResult.getAccessKey().getAccessKeyId(),
        createAccessKeyResult.getAccessKey().getSecretAccessKey()
      )
    );
  }

  public void waitUntilRotatedKeyIsAvailable() throws KeyRotationException {
    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest()
      .withRequestCredentialsProvider(myNewCredentials);
    try {
      new RetrierImpl(myRotateTimeoutSec)
        .registerListener(new DelayListener(1000))
        .execute(() -> mySts.getCallerIdentity(getCallerIdentityRequest));

    } catch (RuntimeException e) {
      throw new KeyRotationException("Rotated connection is invalid after " + myRotateTimeoutSec + " seconds: " + e.getMessage(), e);
    }
  }

  public void updateConnection() throws KeyRotationException {
    OAuthConnectionDescriptor currentConnection = myOAuthConnectionsManager.findConnectionById(myProject, myAwsConnectionDescriptor.getId());
    if (currentConnection == null) {
      throw new KeyRotationException("The connection has been deleted while it was being rotated. Cannot update connection with ID: " + myAwsConnectionDescriptor.getId());
    }

    Map<String, String> newParameters = new HashMap<>(currentConnection.getParameters());
    newParameters.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, myNewCredentials.getCredentials().getAWSAccessKeyId());
    newParameters.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, myNewCredentials.getCredentials().getAWSSecretKey());

    myOAuthConnectionsManager.updateConnection(myProject, currentConnection.getId(), currentConnection.getOauthProvider().getType(), newParameters);
    persist(myProject, "Connection updated");
  }

  public void deletePreviousAccessKey() throws KeyRotationException {
    DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
      .withAccessKeyId(myPreviousCredentials.getCredentials().getAWSAccessKeyId())
      .withRequestCredentialsProvider(myPreviousCredentials);
    try {
      myIam.deleteAccessKey(deleteAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  private void persist(@NotNull final SProject project, @NotNull final String text) {
    final AuthorityHolder authHolder = mySecurityContext.getAuthorityHolder();
    if (authHolder instanceof SUser) {
      project.schedulePersisting(myConfigActionFactory.createAction((SUser) authHolder, project, text));
    } else {
      project.schedulePersisting(myConfigActionFactory.createAction(project, text));
    }
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

  @NotNull
  private String getIamUserName(@NotNull final AWSCredentialsProvider creds) {
    GetUserRequest getUserRequest = new GetUserRequest()
      .withRequestCredentialsProvider(creds);
    return myIam.getUser(getUserRequest).getUser().getUserName();
  }


  @Used("tests")
  public AwsRotateKeyActions(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                             @NotNull final SecurityContextEx securityContext,
                             @NotNull final ConfigActionFactory configActionFactory,
                             @NotNull final OAuthConnectionDescriptor awsConnectionDescriptor,
                             @NotNull final AmazonIdentityManagement iam,
                             @NotNull final AWSSecurityTokenService sts,
                             @NotNull final SProject project) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    myAwsConnectionDescriptor = awsConnectionDescriptor;
    myIam = iam;
    mySts = sts;
    myProject = project;
    myRotateTimeoutSec = 1;

    myPreviousCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
        awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
      )
    );
  }
}
