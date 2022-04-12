package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.intellij.openapi.util.Pair;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.controllers.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AvailableAwsConnsController extends BaseController {
  public static final String PATH = AVAIL_AWS_CONNECTIONS_CONTROLLER_URL;

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String availableAwsConnsBeanName = "awsConnections";

  private final OAuthConnectionsManager myConnectionsManager;
  private final ProjectManager myProjectManager;
  private final PluginDescriptor myDescriptor;

  public AvailableAwsConnsController(@NotNull final SBuildServer server,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                     @NotNull final ProjectManager projectManager,
                                     @NotNull final AuthorizationInterceptor authInterceptor,
                                     @NotNull final PluginDescriptor descriptor) {
    super(server);
    myConnectionsManager = oAuthConnectionsManager;
    myProjectManager = projectManager;
    myDescriptor = descriptor;
    if (TeamCityProperties.getBoolean(FEATURE_PROPERTY_NAME)) {
      final RequestPermissionsChecker projectAccessChecker = (RequestPermissionsCheckerEx)(securityContext, request) -> {
        String projectId = request.getParameter("projectId");
        SProject curProject = myProjectManager.findProjectByExternalId(projectId);
        if (curProject == null) {
          throw new AccessDeniedException(securityContext.getAuthorityHolder(), "Project with id " + request.getParameter("projectId") + " does not exist");
        }
        securityContext.getAccessChecker().checkCanEditProject(curProject);
      };

      webControllerManager.registerController(PATH, this);
      authInterceptor.addPathBasedPermissionsChecker(PATH, projectAccessChecker);
    }
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    Loggers.CLOUD.debug("Available AWS Connections have been requested for the project with id: " + request.getParameter("projectId"));

    final ActionErrors errors = new ActionErrors();

    try {
      final String projectId = request.getParameter("projectId");
      if (projectId == null) {
        throw new AwsConnectorException("The ID of the project where to find Available AWS Connections is null");
      }
      SProject project = myProjectManager.findProjectByExternalId(projectId);
      if (project == null) {
        throw new AwsConnectorException("Could not find the project with id: " + projectId);
      }


      String resourceName = request.getParameter("resource");
      if (resourceName == null) {
        ModelAndView mv = new ModelAndView(myDescriptor.getPluginResourcesPath(AwsConnectionProvider.EDIT_PARAMS_URL));
        mv.getModel().put("projectId", project.getProjectId());
        final List<OAuthConnectionDescriptor> connections = myConnectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE);
        mv.getModel().put(availableAwsConnsBeanName, asPairs(connections, c -> c.getId(), c -> c.getConnectionDisplayName()));

        return mv;

      } else if (resourceName.equals(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME)) {
        List<OAuthConnectionDescriptor> awsConnections = myConnectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE);
        writeAsJson(asPairs(awsConnections, c -> c.getId(), c -> c.getConnectionDisplayName()), response);

      } else {
        throw new AwsConnectorException("Resource " + resourceName + " is not supported. Only " + AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME + " is supported.");
      }

    } catch (AwsConnectorException e) {
      errors.addError("error_" + AVAIL_AWS_CONNECTIONS_SELECT_ID, e.getMessage());
      writeAsJson(errors, response);
    }

    return null;
  }

  private <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
    final String json = OBJECT_MAPPER.writeValueAsString(value);
    response.setContentType("application/json");
    response.setCharacterEncoding(Charsets.UTF_8.name());
    final PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
  }

  @NotNull
  private <T> List<Pair<String, String>> asPairs(@NotNull List<T> values, @NotNull Function<T, String> getValue, @NotNull Function<T, String> getLabel) {
    return values.stream().map(c -> new Pair<String, String>(getValue.apply(c), getLabel.apply(c))).collect(Collectors.toList());
  }
}

