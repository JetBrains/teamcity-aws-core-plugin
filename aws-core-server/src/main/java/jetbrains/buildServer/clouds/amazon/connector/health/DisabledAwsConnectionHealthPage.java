package jetbrains.buildServer.clouds.amazon.connector.health;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.serverSide.ProjectManagerEx;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisabledAwsConnectionHealthPage extends HealthStatusItemPageExtension {

  public static final String DISABLED_AWS_CONNECTION_HEALTH_PAGE_RESOURCE_PATH = "awsConnection/health/disabledAwsConnectionReport.jsp";

  private final ProjectManagerEx myProjectManager;

  public DisabledAwsConnectionHealthPage(@NotNull PluginDescriptor pluginDescriptor,
                                         @NotNull final PagePlaces pagePlaces,
                                         @NotNull final ProjectManagerEx projectManager) {
    super(DisabledAwsConnectionHealthReport.REPORT_TYPE, pagePlaces);
    myProjectManager = projectManager;
    setIncludeUrl(pluginDescriptor.getPluginResourcesPath(DISABLED_AWS_CONNECTION_HEALTH_PAGE_RESOURCE_PATH));

    setVisibleOutsideAdminArea(false);
    register();
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    HealthStatusItem item = getStatusItem(request);
    Object disabledAwsConnections = item.getAdditionalData().get(DisabledAwsConnectionHealthReport.DISABLED_AWS_CONNECTIONS_PARAM);

    SProject project = getProject(request);

    return super.isAvailable(request) &&
           project != null &&
           disabledAwsConnections != null &&
           SessionUser.getUser(request).isPermissionGrantedForProject(project.getProjectId(), Permission.EDIT_PROJECT);
  }

  @Nullable
  private ProjectEx getProject(@NotNull HttpServletRequest request) {
    String projectId = StringUtil.emptyIfNull(request.getParameter("projectId"));
    return myProjectManager.findProjectByExternalId(projectId);
  }
}
