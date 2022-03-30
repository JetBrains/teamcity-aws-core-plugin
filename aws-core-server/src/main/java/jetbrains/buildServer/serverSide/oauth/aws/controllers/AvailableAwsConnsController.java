package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.ProjectAccessChecker;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AvailableAwsConnsController extends BaseFormXmlController {
  public static final String PATH = AVAIL_AWS_CONNECTIONS_CONTROLLER_URL;

  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final ProjectManager myProjectManager;

  public AvailableAwsConnsController(@NotNull final SBuildServer server,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                     @NotNull final ProjectManager projectManager,
                                     @NotNull final AuthorizationInterceptor authInterceptor) {
    super(server);
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myProjectManager = projectManager;
    if (TeamCityProperties.getBoolean(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(PATH, this);
      authInterceptor.addPathBasedPermissionsChecker(PATH, new ProjectAccessChecker(myProjectManager));
    }
  }

  @Override
  protected void doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {

    String projectId = request.getParameter("projectId");
    SProject project = getCurrentProject(projectId);

    ActionErrors errors = new ActionErrors();

    if (project == null) {
      errors.addError("error_availAwsConnections", "Failed to find project with id: " + projectId);
      writeErrors(xmlResponse, errors);
      return;
    }

    List<OAuthConnectionDescriptor> awsConnections = myOAuthConnectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE);

    Element awsConnectionsElements = awsConnectionsToElement(awsConnections);
    xmlResponse.addContent((Content)awsConnectionsElements);
  }

  private SProject getCurrentProject(String projectId) {
    return myProjectManager.findProjectByExternalId(projectId);
  }

  private Element awsConnectionsToElement(List<OAuthConnectionDescriptor> awsConnections) {

    Element availConnectionsElement = new Element(AVAIL_AWS_CONNECTIONS_ELEMENT);
    for (OAuthConnectionDescriptor awsConnection : awsConnections) {
      Element elem = new Element(AWS_CONNECTION_ELEMENT);
      elem.setAttribute(AWS_CONNECTION_ATTR_NAME, awsConnection.getConnectionDisplayName());
      elem.setAttribute(AWS_CONNECTION_ATTR_ID, awsConnection.getId());
      elem.setAttribute(AWS_CONNECTION_ATTR_DESCRIPTION, awsConnection.getDescription());
      elem.setAttribute(AWS_CONNECTION_ATTR_OWN_PROJ_ID, awsConnection.getProject().getProjectId());
      availConnectionsElement.addContent((Content)elem);
    }
    return availConnectionsElement;
  }

  @Override
  protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
    return null;
  }
}

