package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AvailableAwsConnsController extends BaseAwsConnectionController {
  public static final String PATH = AVAIL_AWS_CONNECTIONS_CONTROLLER_URL;
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
    super(PATH, server, projectManager, webControllerManager, authInterceptor);
    myConnectionsManager = oAuthConnectionsManager;
    myProjectManager = projectManager;
    myDescriptor = descriptor;
  }

  public static List<List<String>> getAvailableAwsConnectionsParams(List<OAuthConnectionDescriptor> awsConnections) {
    List<List<String>> res = new ArrayList<>();
    for (OAuthConnectionDescriptor awsConnection : awsConnections) {
      List<String> props = new ArrayList<>();
      props.add(awsConnection.getId());
      props.add(awsConnection.getConnectionDisplayName());
      props.add(ParamUtil.useSessionCredentials(awsConnection.getParameters()) ? "true" : "false");
      res.add(props);
    }
    return res;
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

      List<OAuthConnectionDescriptor> awsConnections = myConnectionsManager.getAvailableConnectionsOfType(project, AwsConnectionProvider.TYPE);

      final boolean forBuildStepFeatureEnabled = Boolean.parseBoolean(project.getParameterValue(ALLOWED_IN_BUILDS_FEATURE_FLAG));
      final boolean isForBuildStep = Boolean.parseBoolean(request.getParameter(ALLOWED_IN_BUILDS_REQUEST_PARAM));
      if (forBuildStepFeatureEnabled && isForBuildStep) {
        awsConnections = awsConnections.stream().filter(conn -> {
          final String isAllowedInBuildSteps = conn.getParameters().get(AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_PARAM);
          return isAllowedInBuildSteps == null || Boolean.parseBoolean(isAllowedInBuildSteps);
        }).collect(Collectors.toList());
      }

      final boolean childProjectsFeatureEnabled = Boolean.parseBoolean(project.getParameterValue(ALLOWED_IN_SUBPROJECTS_FEATURE_FLAG));

      if (childProjectsFeatureEnabled) {
        awsConnections = awsConnections.stream()
                                       .filter(conn -> conn.getProjectId().equals(projectId) || ParamUtil.isAllowedInSubProjects(conn.getParameters()))
                                       .collect(Collectors.toList());
      }

      List<List<String>> readyAvailAwsConnProps = getAvailableAwsConnectionsParams(processAvailableAwsConnections(awsConnections, request));

      String resourceName = request.getParameter("resource");
      if (resourceName == null) {
        ModelAndView mv = new ModelAndView(myDescriptor.getPluginResourcesPath(AwsConnectionProvider.EDIT_PARAMS_URL));
        mv.getModel().put("projectId", project.getProjectId());
        mv.getModel().put(availableAwsConnsBeanName, readyAvailAwsConnProps);
        return mv;

      } else if (resourceName.equals(AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME)) {
        writeAsJson(
          readyAvailAwsConnProps,
          response
        );

      } else {
        throw new AwsConnectorException("Resource " + resourceName + " is not supported. Only " + AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME + " is supported.");
      }

    } catch (AwsConnectorException e) {
      errors.addError("error_" + AVAIL_AWS_CONNECTIONS_SELECT_ID, e.getMessage());
      writeAsJson(errors, response);
    }

    return null;
  }

  private List<OAuthConnectionDescriptor> processAvailableAwsConnections(List<OAuthConnectionDescriptor> awsConnections, HttpServletRequest request) {
    String principalAwsConnId = request.getParameter(PRINCIPAL_AWS_CONNECTION_ID);
    if(StringUtil.nullIfEmpty(principalAwsConnId) != null){
      return awsConnections.stream().filter(connectionDescriptor -> {
        return !connectionDescriptor.getId().equals(principalAwsConnId);
      }).collect(Collectors.toList());
    }

    return awsConnections;
  }
}

