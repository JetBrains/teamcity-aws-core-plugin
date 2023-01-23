package jetbrains.buildServer.clouds.amazon.connector.health;

import com.intellij.openapi.util.Pair;
import java.util.*;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.healthStatus.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.util.filters.FilterUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.DISABLED_AWS_CONNECTION_TYPE_ERROR_MSG;

public class DisabledAwsConnectionHealthReport extends HealthStatusReport {

  public static final String DISABLED_AWS_CONNECTIONS_PARAM = "disabledAwsConns";

  private static final String PREFIX = "disabledAwsConnection";
  static final String REPORT_TYPE = PREFIX + "HealthReport";
  private static final ItemCategory CATEGORY =
    new ItemCategory(PREFIX + "HealthCategory", "AWS Connection is disabled", ItemSeverity.WARN);

  private final OAuthConnectionsManager myOAuthConnectionsManager;

  public DisabledAwsConnectionHealthReport(@NotNull final OAuthConnectionsManager oAuthConnectionsManager) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
  }

  @NotNull
  @Override
  public String getType() {
    return REPORT_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return DISABLED_AWS_CONNECTION_TYPE_ERROR_MSG;
  }

  @NotNull
  @Override
  public Collection<ItemCategory> getCategories() {
    return Collections.singletonList(CATEGORY);
  }

  @Override
  public boolean canReportItemsFor(@NotNull final HealthStatusScope scope) {
    return scope.isItemWithSeverityAccepted(ItemSeverity.WARN);
  }

  @Override
  public void report(@NotNull HealthStatusScope scope, @NotNull HealthStatusItemConsumer resultConsumer) {
    if (! needToReport()) {
      return;
    }

    for (SProject project: scope.getProjects()) {
      ArrayList<Pair<String, String>> disabledProjectConnIdsPairs = new ArrayList<>();

      List<OAuthConnectionDescriptor> awsConnections = FilterUtil.filterCollection(myOAuthConnectionsManager.getOwnAvailableConnections(project), new Filter<OAuthConnectionDescriptor>() {
        @Override
        public boolean accept(@NotNull final OAuthConnectionDescriptor data) {
          return data.getOauthProvider().getType().equals(AwsConnectionProvider.TYPE);
        }
      });

      for (OAuthConnectionDescriptor awsConnection : awsConnections) {
        if (ParamUtil.isDefaultCredsProviderType(awsConnection.getParameters())) {
          disabledProjectConnIdsPairs.add(new Pair<>(project.getExternalId(), awsConnection.getId()));
        }
      }

      if (disabledProjectConnIdsPairs.isEmpty()) {
        continue;
      }

      Map<String, Object> data = new HashMap<>();
      data.put(DISABLED_AWS_CONNECTIONS_PARAM, disabledProjectConnIdsPairs);
      resultConsumer.consumeForProject(project, new HealthStatusItem(PREFIX + "HealthItemId", CATEGORY, ItemSeverity.WARN, data));
    }
  }

  private boolean needToReport() {
    return ParamUtil.isDefaultCredsProvidertypeDisabled();
  }
}