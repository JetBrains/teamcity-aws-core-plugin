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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.testUtils.AbstractControllerTest;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static org.mockito.Mockito.when;

public class AwsRotateKeysControllerTest extends AbstractControllerTest {

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String AWS_CONNECTION_ID = "PROJECT_FEATURE_ID";
  private final String AWS_ACCESS_KEY_ID = "ACCESSKEY";
  private final String AWS_SECRET_ACCESS_KEY = "SECRETKEY";
  private final String ROTATED_AWS_ACCESS_KEY_ID = "ACCESSKEYROTATED";
  private final String ROTATED_AWS_SECRET_ACCESS_KEY = "SECRETKEYROTATED";
  private AwsRotateKeysController myAwsRotateKeysController;
  private OAuthConnectionsManager connectionsManager;
  private OAuthConnectionDescriptor mockedAwsConnection;

  @BeforeMethod
  public void setUp() throws IOException {
    super.setUp();

    setInternalProperty(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME, "true");

    SProject project = Mockito.mock(SProject.class);
    when(projectManager.findProjectByExternalId(PROJECT_ID))
      .thenReturn(project);

    mockedAwsConnection = Mockito.mock(OAuthConnectionDescriptor.class);
    when(mockedAwsConnection.getId())
      .thenReturn(AWS_CONNECTION_ID);
    when(mockedAwsConnection.getParameters())
      .thenReturn(createDefaultProperties());

    connectionsManager = Mockito.mock(OAuthConnectionsManager.class);
    when(connectionsManager.findConnectionById(project, mockedAwsConnection.getId()))
      .thenReturn(mockedAwsConnection);

    myAwsRotateKeysController = new AwsRotateKeysController(
      Mockito.mock(SBuildServer.class),
      Mockito.mock(WebControllerManager.class),
      projectManager,
      Mockito.spy(AuthorizationInterceptor.class),
      createRotator()
    );
  }

  private Map<String, String> createDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, AWS_ACCESS_KEY_ID);
    res.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, AWS_SECRET_ACCESS_KEY);
    res.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    res.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    return res;
  }

  private AwsKeyRotator createRotator() {
    return (connectionId, project) -> {
      OAuthConnectionDescriptor connection = connectionsManager.findConnectionById(project, connectionId);
      if (connection == null) {
        throw new AwsConnectorException("Connection to rotate was not found, ID is: " + connectionId);
      }

      Map<String, String> rotatedProperties = createDefaultProperties();
      rotatedProperties.put(ACCESS_KEY_ID_PARAM, ROTATED_AWS_ACCESS_KEY_ID);
      rotatedProperties.put(SECURE_SECRET_ACCESS_KEY_PARAM, ROTATED_AWS_SECRET_ACCESS_KEY);

      when(connection.getParameters())
        .thenReturn(rotatedProperties);
    };
  }

  @Test
  public void givenAllParams_withRotateKeyRequest_thenReturnRotatedKey() throws JsonProcessingException {
    when(request.getParameter("connectionId"))
      .thenReturn(AWS_CONNECTION_ID);

    try {
      myAwsRotateKeysController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();
    ActionErrors expectedErrors = new ActionErrors();
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
    assertEquals(ROTATED_AWS_ACCESS_KEY_ID, mockedAwsConnection.getParameters().get(ACCESS_KEY_ID_PARAM));
    assertEquals(ROTATED_AWS_SECRET_ACCESS_KEY, mockedAwsConnection.getParameters().get(SECURE_SECRET_ACCESS_KEY_PARAM));
  }

  @Test
  public void givenAllParams_withNullProjectId_thenReturnError() throws JsonProcessingException {
    when(request.getParameter("projectId"))
      .thenReturn(null);

    try {
      myAwsRotateKeysController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();
    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: The ID of the project where to rotate keys is null.");
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
    assertEquals(AWS_ACCESS_KEY_ID, mockedAwsConnection.getParameters().get(ACCESS_KEY_ID_PARAM));
    assertEquals(AWS_SECRET_ACCESS_KEY, mockedAwsConnection.getParameters().get(SECURE_SECRET_ACCESS_KEY_PARAM));
  }

  @Test
  public void givenAllParams_withNonExistingProject_thenReturnError() throws JsonProcessingException {
    when(request.getParameter("projectId"))
      .thenReturn("NONEXISTING");

    try {
      myAwsRotateKeysController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();
    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: Could not find the project with id: NONEXISTING");
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
    assertEquals(AWS_ACCESS_KEY_ID, mockedAwsConnection.getParameters().get(ACCESS_KEY_ID_PARAM));
    assertEquals(AWS_SECRET_ACCESS_KEY, mockedAwsConnection.getParameters().get(SECURE_SECRET_ACCESS_KEY_PARAM));
  }

  @Test
  public void givenAllParams_withNonExistingConnection_thenReturnError() throws JsonProcessingException {
    when(request.getParameter("connectionId"))
      .thenReturn("NONEXISTING");

    try {
      myAwsRotateKeysController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();
    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError(ROTATE_KEY_BTTN_ID, "Unable to rotate keys: Connection to rotate was not found, ID is: NONEXISTING");
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
    assertEquals(AWS_ACCESS_KEY_ID, mockedAwsConnection.getParameters().get(ACCESS_KEY_ID_PARAM));
    assertEquals(AWS_SECRET_ACCESS_KEY, mockedAwsConnection.getParameters().get(SECURE_SECRET_ACCESS_KEY_PARAM));
  }
}