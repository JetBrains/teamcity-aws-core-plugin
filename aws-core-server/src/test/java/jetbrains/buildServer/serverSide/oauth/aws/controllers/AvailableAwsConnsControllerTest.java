

package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.testUtils.AbstractControllerTest;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.testUtils.TestUtils.createConnectionDescriptor;
import static org.mockito.Mockito.when;

public class AvailableAwsConnsControllerTest extends AbstractControllerTest {

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String UNKNOWN_RESOURCE = "UNKNOWN";
  private final String AWS_CONNECTION_ID = "PROJECT_FEATURE_ID";
  private final String AWS_CONNECTION_DISPLAY_NAME = "Test AWS Connection";
  private AvailableAwsConnsController availableAwsConnsController;
  private ProjectConnectionsManager connectionsManager;
  private SProject project;
  private ConnectionDescriptor mockedAwsConnection;

  @BeforeMethod
  public void setUp() throws IOException {
    super.setUp();

    setInternalProperty(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME, "true");

    project = Mockito.mock(SProject.class);
    when(projectManager.findProjectByExternalId(PROJECT_ID))
      .thenReturn(project);

    connectionsManager = Mockito.mock(ProjectConnectionsManager.class);

    availableAwsConnsController = new AvailableAwsConnsController(
      Mockito.mock(SBuildServer.class),
      Mockito.mock(WebControllerManager.class),
      connectionsManager,
      projectManager,
      Mockito.spy(AuthorizationInterceptor.class),
      Mockito.mock(PluginDescriptor.class)
    );


    mockedAwsConnection = Mockito.mock(ConnectionDescriptor.class);
    when(mockedAwsConnection.getProjectId()).thenReturn(PROJECT_ID);
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
    expectedErrors.addError("error_" + AVAIL_AWS_CONNECTIONS_SELECT_ID,
                            "Resource " + UNKNOWN_RESOURCE + " is not supported. Only " + AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME + " is supported.");
    String expectedErrorsJson = OBJECT_MAPPER.writeValueAsString(expectedErrors);

    assertEquals(expectedErrorsJson, result);
  }

  @Test
  public void givenAllParams_withExistingAwsConnection_thenReturnThisAwsConnection() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);

    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(Collections.singletonList(mockedAwsConnection));

    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(AvailableAwsConnsController.getAvailableAwsConnectionsParams(Collections.singletonList(mockedAwsConnection)));

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

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(AvailableAwsConnsController.getAvailableAwsConnectionsParams(Collections.emptyList()));

    assertEquals(expectedResponseJson, result);
  }

  @Test
  public void givenAllParams_withSeveralAwsConnection_thenReturnThemAll() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);

    List<ConnectionDescriptor> availableAsConnections = Arrays.asList(mockedAwsConnection, mockedAwsConnection, mockedAwsConnection);
    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(availableAsConnections);

    when(mockedAwsConnection.getId())
      .thenReturn(AWS_CONNECTION_ID);
    when(mockedAwsConnection.getDisplayName())
      .thenReturn(AWS_CONNECTION_DISPLAY_NAME);


    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(AvailableAwsConnsController.getAvailableAwsConnectionsParams(availableAsConnections));

    assertEquals(expectedResponseJson, result);
  }

  @Test
  public void givenIsForBuild_withSeveralAwsConnection_thenReturnOnlyAllowedForBuilds() throws JsonProcessingException {
    when(request.getParameter("resource"))
      .thenReturn(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME);
    when(request.getParameter("forBuildStep"))
      .thenReturn("true");


    final Map<String, String> params = new HashMap<>();
    params.put(ALLOWED_IN_BUILDS_PARAM, "false");

    ConnectionDescriptor mockedAwsConnection1 = createConnectionDescriptor(PROJECT_ID, AWS_CONNECTION_ID + 1, params);

    final Map<String, String> params2 = new HashMap<>();
    params2.put(ALLOWED_IN_BUILDS_PARAM, "true");

    ConnectionDescriptor mockedAwsConnection2 = createConnectionDescriptor(PROJECT_ID, AWS_CONNECTION_ID + 2, params2);


    List<ConnectionDescriptor> availableAwsConnections = Arrays.asList(mockedAwsConnection1, mockedAwsConnection2);

    when(connectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE))
      .thenReturn(availableAwsConnections);

    try {
      availableAwsConnsController.doHandle(request, response);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

    String result = responseOutputStream.toString();

    String expectedResponseJson = OBJECT_MAPPER.writeValueAsString(AvailableAwsConnsController.getAvailableAwsConnectionsParams(availableAwsConnections));

    assertEquals(expectedResponseJson, result);
  }

  @Test
  public void givenAwsConn_withDefaultCredsProviderType_thenReturnParamsWithSessionDurationOff() {
    final Map<String, String> params = new HashMap<>();
    params.put(CREDENTIALS_TYPE_PARAM, DEFAULT_PROVIDER_CREDENTIALS_TYPE);
    ConnectionDescriptor mockedAwsConnection1 = createConnectionDescriptor(PROJECT_ID, AWS_CONNECTION_ID + 1, params);

    final Map<String, String> params2 = new HashMap<>();
    params2.put(CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE);
    params2.put(SESSION_CREDENTIALS_PARAM, "true");
    ConnectionDescriptor mockedAwsConnection2 = createConnectionDescriptor(PROJECT_ID, AWS_CONNECTION_ID + 2, params2);

    List<ConnectionDescriptor> availableAwsConnections = Arrays.asList(mockedAwsConnection1, mockedAwsConnection2);

    String expectedResult = Arrays.asList(
      Arrays.asList(mockedAwsConnection1.getId(), "", "false"),
      Arrays.asList(mockedAwsConnection2.getId(), "", "true")
    ).toString();

    assertEquals(
      expectedResult,
      AvailableAwsConnsController
        .getAvailableAwsConnectionsParams(availableAwsConnections)
        .toString()
    );
  }
}