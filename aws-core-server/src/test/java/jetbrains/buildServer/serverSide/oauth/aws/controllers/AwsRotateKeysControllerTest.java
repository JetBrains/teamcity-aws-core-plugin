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

package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl.AwsKeyRotatorImpl;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class AwsRotateKeysControllerTest extends BaseControllerTestCase<AwsRotateKeysController> {

  private final String TEST_CONN_FEATURE_ID = "EXT_1";

  private final String CURRENT_ACCESS_KEY = "EXAMPLE_ACCESS";
  private final String CURRENT_SECRET_KEY = "EXAMPLE_SECRET";
  private final String ROTATED_ACCESS_KEY = "ROTATED_ACCESS";
  private final String ROTATED_SECRET_KEY = "ROTATED_SECRET";

  private final String TEST_USER_NAME = "SomeUser";

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private OAuthConnectionsManager myOAuthConnectionsManager;
  private AmazonIdentityManagement iam;
  private AWSSecurityTokenService sts;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();

    myProject = myFixture.createProject("AWS Connection Key Rotation Test Project");
    addTestConnection();
  }

  private void addTestConnection() {
    Map<String, String> projectFeatureProperties = new HashMap<>();
    projectFeatureProperties.put(OAuthConstants.OAUTH_TYPE_PARAM, AwsConnectionProvider.TYPE);
    projectFeatureProperties.put(ACCESS_KEY_ID_PARAM, CURRENT_ACCESS_KEY);
    projectFeatureProperties.put(SECURE_SECRET_ACCESS_KEY_PARAM, CURRENT_SECRET_KEY);

    ProjectFeatureDescriptorImpl projectFeature = new ProjectFeatureDescriptorImpl(
      TEST_CONN_FEATURE_ID,
      OAuthConstants.FEATURE_TYPE,
      projectFeatureProperties,
      myProject.getProjectId()
    );
    myProject.addFeature(projectFeature);
  }

  @Override
  protected AwsRotateKeysController createController() {
    iam = Mockito.mock(AmazonIdentityManagement.class);
    sts = Mockito.mock(AWSSecurityTokenService.class);
    myOAuthConnectionsManager = new OAuthConnectionsManager(myServer);
    return new AwsRotateKeysController(
      myServer,
      myWebManager,
      myProjectManager,
      getWebFixture().getAuthorizationInterceptor(),
      createKeyRotator()
    );
  }

  private AwsKeyRotator createKeyRotator() {

    when(iam.getUser(any()))
      .thenReturn(new GetUserResult()
        .withUser(new User()
          .withUserName(TEST_USER_NAME)));

    when(iam.createAccessKey(any()))
      .thenReturn(new CreateAccessKeyResult()
        .withAccessKey(new AccessKey()
          .withAccessKeyId(ROTATED_ACCESS_KEY)
          .withSecretAccessKey(ROTATED_SECRET_KEY)));

    when(sts.getCallerIdentity(any()))
      .thenReturn(
        new GetCallerIdentityResult());

    return new AwsKeyRotatorImpl(
      myOAuthConnectionsManager,
      myFixture.getEventDispatcher(),
      myFixture.getSecurityContext(),
      myFixture.getConfigActionFactory(),
      iam,
      sts
    );
  }

  @Test
  public void givenProjectAndConnection_whenRotationRequested_thenReturnRotatedConnection() throws Exception {

    doPost("projectId", myProject.getExternalId(),
      "connectionId", TEST_CONN_FEATURE_ID);

    ActionErrors expectedErrors = new ActionErrors();
    assertEquals(OBJECT_MAPPER.writeValueAsString(expectedErrors), myResponse.getReturnedContent());

    OAuthConnectionDescriptor connection = myOAuthConnectionsManager.findConnectionById(myProject, TEST_CONN_FEATURE_ID);
    if (connection == null)
      fail("Rotated connection was not found");

    assertEquals(
      ROTATED_ACCESS_KEY,
      connection
        .getParameters()
        .get(ACCESS_KEY_ID_PARAM)
    );

    assertEquals(
      ROTATED_SECRET_KEY,
      connection
        .getParameters()
        .get(SECURE_SECRET_ACCESS_KEY_PARAM)
    );

    Mockito.verify(iam, times(1)).deleteAccessKey(new DeleteAccessKeyRequest()
      .withAccessKeyId(CURRENT_ACCESS_KEY)
      .withRequestCredentialsProvider(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
        ROTATED_ACCESS_KEY,
        ROTATED_SECRET_KEY
      )))
    );
  }

  @Test
  public void givenProjectAndNonExistingConnection_whenRotationRequested_thenReturnError() throws Exception {

    doPost("projectId", myProject.getExternalId(),
      "connectionId", "NON_EXISING");

    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: The AWS Connection with ID NON_EXISING was not found.");
    assertEquals(OBJECT_MAPPER.writeValueAsString(expectedErrors), myResponse.getReturnedContent());

    OAuthConnectionDescriptor connection = myOAuthConnectionsManager.findConnectionById(myProject, TEST_CONN_FEATURE_ID);
    if (connection == null)
      fail("Rotated connection was not found");

    assertEquals(
      CURRENT_ACCESS_KEY,
      connection
        .getParameters()
        .get(ACCESS_KEY_ID_PARAM)
    );

    assertEquals(
      CURRENT_SECRET_KEY,
      connection
        .getParameters()
        .get(SECURE_SECRET_ACCESS_KEY_PARAM)
    );

    Mockito.verify(iam, Mockito.never()).deleteAccessKey(any());
  }

  @Test
  public void givenProjectAndConnection_whenAwsRotateRequestFailed_thenReturnError() throws Exception {

    when(iam.createAccessKey(any()))
      .thenThrow(new LimitExceededException("There are 2 access key already."));

    doPost("projectId", myProject.getExternalId(),
      "connectionId", TEST_CONN_FEATURE_ID);

    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: There are 2 access key already.");
    assertEquals(OBJECT_MAPPER.writeValueAsString(expectedErrors), myResponse.getReturnedContent());

    OAuthConnectionDescriptor connection = myOAuthConnectionsManager.findConnectionById(myProject, TEST_CONN_FEATURE_ID);
    if (connection == null)
      fail("Rotated connection was not found");

    assertEquals(
      CURRENT_ACCESS_KEY,
      connection
        .getParameters()
        .get(ACCESS_KEY_ID_PARAM)
    );

    assertEquals(
      CURRENT_SECRET_KEY,
      connection
        .getParameters()
        .get(SECURE_SECRET_ACCESS_KEY_PARAM)
    );

    Mockito.verify(iam, Mockito.never()).deleteAccessKey(any());
  }

  @Test
  public void givenProjectAndConnection_whenAwsRotateRequestTimedOut_thenReturnError() throws Exception {

    when(sts.getCallerIdentity(any()))
      .thenThrow(new RuntimeException("Dummy timeout."));

    doPost("projectId", myProject.getExternalId(),
      "connectionId", TEST_CONN_FEATURE_ID);

    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: Rotated connection is invalid after 1 seconds: Dummy timeout.");
    assertEquals(OBJECT_MAPPER.writeValueAsString(expectedErrors), myResponse.getReturnedContent());

    OAuthConnectionDescriptor connection = myOAuthConnectionsManager.findConnectionById(myProject, TEST_CONN_FEATURE_ID);
    if (connection == null)
      fail("Rotated connection was not found");

    assertEquals(
      CURRENT_ACCESS_KEY,
      connection
        .getParameters()
        .get(ACCESS_KEY_ID_PARAM)
    );

    assertEquals(
      CURRENT_SECRET_KEY,
      connection
        .getParameters()
        .get(SECURE_SECRET_ACCESS_KEY_PARAM)
    );

    Mockito.verify(iam, Mockito.never()).deleteAccessKey(any());
  }
}