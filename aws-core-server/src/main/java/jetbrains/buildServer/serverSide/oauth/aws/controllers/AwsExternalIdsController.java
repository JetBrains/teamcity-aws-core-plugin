package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.EXTERNAL_IDS_CONTROLLER_URL;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.EXTERNAL_ID_FIELD_ID;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AWS_CONN_ID_REST_PARAM;

public class AwsExternalIdsController extends BaseAwsConnectionController {
  public static final String PATH = EXTERNAL_IDS_CONTROLLER_URL;

  private final ProjectManager myProjectManager;
  private final AwsExternalIdsManager myAwsExternalIdsManager;

  public AwsExternalIdsController(@NotNull final SBuildServer server,
                                  @NotNull final WebControllerManager webControllerManager,
                                  @NotNull final ProjectManager projectManager,
                                  @NotNull final AuthorizationInterceptor authInterceptor,
                                  @NotNull final AwsExternalIdsManager awsExternalIdsManager) {
    super(PATH, server, projectManager, webControllerManager, authInterceptor);
    myProjectManager = projectManager;
    myAwsExternalIdsManager = awsExternalIdsManager;
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final String connectionId = request.getParameter(AWS_CONN_ID_REST_PARAM);
    final String projectId = request.getParameter("projectId");
    Loggers.CLOUD.debug(String.format("AWS Connection External Id was requested for connection with id: %s in the Project with id: %s", connectionId, projectId));

    final ActionErrors errors = new ActionErrors();

    SProject project = myProjectManager.findProjectByExternalId(projectId);
    if (project != null) {
      writeAsJson(
        myAwsExternalIdsManager
          .getAwsConnectionExternalId(connectionId, project.getProjectId()),
        response
      );
    } else {
      errors.addError("error_" + EXTERNAL_ID_FIELD_ID, "There is no project with ID: " + projectId);
      writeAsJson(errors, response);
    }

    return null;
  }
}