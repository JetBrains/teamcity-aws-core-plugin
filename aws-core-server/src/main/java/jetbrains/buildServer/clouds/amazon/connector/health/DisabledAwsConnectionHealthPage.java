package jetbrains.buildServer.clouds.amazon.connector.health;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension;
import org.jetbrains.annotations.NotNull;

public class DisabledAwsConnectionHealthPage extends HealthStatusItemPageExtension {

  public static final String DISABLED_AWS_CONNECTION_HEALTH_PAGE_RESOURCE_PATH = "awsConnection/health/disabledAwsConnectionReport.jsp";

  public DisabledAwsConnectionHealthPage(@NotNull PluginDescriptor pluginDescriptor,
                                         @NotNull final PagePlaces pagePlaces) {
    super(DisabledAwsConnectionHealthReport.REPORT_TYPE, pagePlaces);
    setIncludeUrl(pluginDescriptor.getPluginResourcesPath(DISABLED_AWS_CONNECTION_HEALTH_PAGE_RESOURCE_PATH));

    setVisibleOutsideAdminArea(false);
    register();
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    HealthStatusItem item = getStatusItem(request);
    Object disabledAwsConnections = item.getAdditionalData().get(DisabledAwsConnectionHealthReport.DISABLED_AWS_CONNECTIONS_PARAM);
    return super.isAvailable(request) && disabledAwsConnections != null;
  }
}
