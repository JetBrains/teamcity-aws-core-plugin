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
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.testUtils.AbstractControllerTest;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNECTIONS_SELECT_ID;
import static org.mockito.Mockito.when;

public class AvailableAwsConnsControllerTest extends AbstractControllerTest {

  private AvailableAwsConnsController availableAwsConnsController;

  private OAuthConnectionsManager connectionsManager;
  private SProject project;

  private OAuthConnectionDescriptor mockedAwsConnection;


  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String UNKNOWN_RESOURCE = "UNKNOWN";
  private final String AWS_CONNECTION_ID = "PROJECT_FEATURE_ID";
  private final String AWS_CONNECTION_DISPLAY_NAME = "Test AWS Connection";

  @BeforeMethod
  public void setUp() throws IOException {
    super.setUp();

    setInternalProperty(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME, "true");

    project = Mockito.mock(SProject.class);
    when(projectManager.findProjectByExternalId(PROJECT_ID))
      .thenReturn(project);

    connectionsManager = Mockito.mock(OAuthConnectionsManager.class);

    availableAwsConnsController = new AvailableAwsConnsController(
      Mockito.mock(SBuildServer.class),
      Mockito.mock(WebControllerManager.class),
      connectionsManager,
      projectManager,
      Mockito.spy(AuthorizationInterceptor.class),
      Mockito.mock(PluginDescriptor.class)
    );


    mockedAwsConnection = Mockito.mock(OAuthConnectionDescriptor.class);
  }

  @Test
  public void givenAllParams_withUnknownResourceName_thenReturnCorrespondingError() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(UNKNOWN_RESOURCE);

    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();
    ActionErrors expectedErrors = new ActionErrors();
    expectedErrors.addError("error_" + AVAIL_AWS_CONNECTIONS_SELECT_ID, "Resource " + UNKNOWN_RESOURCE + " is not supported. Only " + AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME + " is supported.");
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
  }

  @Test
  public void givenAllParams_withExistingAwsConnection_thenReturnThisAwsConnection() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);

    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(Collections.singletonList(mockedAwsConnection));

    when(mockedAwsConnection.getId())
      .thenReturn(AWS_CONNECTION_ID);
    when(mockedAwsConnection.getConnectionDisplayName())
      .thenReturn(AWS_CONNECTION_DISPLAY_NAME);


    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(availableAwsConnsController.asPairs(Collections.singletonList(mockedAwsConnection), OAuthConnectionDescriptor::getId, OAuthConnectionDescriptor::getConnectionDisplayName));

    assertEquals(expectedResponseJson, result);
  }

  @Test
  public void givenAllParams_withNoAwsConnections_thenReturnEmptyResponse() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);

    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(new ArrayList<>());


    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(new ArrayList<>());

    assertEquals(expectedResponseJson, result);
  }

  @Test
  public void givenAllParams_withSeveralAwsConnection_thenReturnThemAll() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);

    List<OAuthConnectionDescriptor> availableAsConnections = Arrays.asList(mockedAwsConnection, mockedAwsConnection, mockedAwsConnection);
    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(availableAsConnections);

    when(mockedAwsConnection.getId())
      .thenReturn(AWS_CONNECTION_ID);
    when(mockedAwsConnection.getConnectionDisplayName())
      .thenReturn(AWS_CONNECTION_DISPLAY_NAME);


    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(availableAwsConnsController.asPairs(availableAsConnections, OAuthConnectionDescriptor::getId, OAuthConnectionDescriptor::getConnectionDisplayName));

    assertEquals(expectedResponseJson, result);
  }
}