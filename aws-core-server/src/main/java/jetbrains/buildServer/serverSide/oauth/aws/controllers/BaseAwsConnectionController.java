

package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME;

public abstract class BaseAwsConnectionController extends BaseController {

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final ProjectManager myProjectManager;
  private final SecurityContextEx mySecurityContext;

  public BaseAwsConnectionController(@NotNull final String controllerPath,
                                     @NotNull final SBuildServer server,
                                     @NotNull final ProjectManager projectManager,
                                     @NotNull final WebControllerManager webControllerManager,
                                     @NotNull final SecurityContextEx securityContext){
    super(server);
    myProjectManager = projectManager;
    mySecurityContext = securityContext;
    if (TeamCityProperties.getBooleanOrTrue(FEATURE_PROPERTY_NAME)) {
      webControllerManager.registerController(controllerPath, this);
    }
  }

  protected final void checkPermissions(@NotNull final HttpServletRequest request) {
    final String projectId = request.getParameter("projectId");
    final SProject project = myProjectManager.findProjectByExternalId(projectId);
    if (project == null) {
      throw new AccessDeniedException(mySecurityContext.getAuthorityHolder(), "Project with id " + projectId + " does not exist");
    }
    mySecurityContext.getAccessChecker().checkCanEditProject(project);
  }

  protected <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
    final String json = OBJECT_MAPPER.writeValueAsString(value);
    response.setContentType("application/json");
    response.setCharacterEncoding(Charsets.UTF_8.name());
    final PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
  }
}
