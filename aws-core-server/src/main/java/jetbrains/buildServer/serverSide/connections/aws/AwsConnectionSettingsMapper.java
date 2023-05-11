
package jetbrains.buildServer.serverSide.connections.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionSettingsMapper implements CustomSettingsMapper {

  @NotNull private final ProjectManager myProjectManager;

  public AwsConnectionSettingsMapper(@NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  @Override
  public void mapData(@NotNull final CopiedObjects copiedObjects) {
    final List<Map.Entry<SProjectFeatureDescriptor, SProjectFeatureDescriptor>> newAwsConnections = copiedObjects
      .getCopiedProjectFeatureDescriptorsMap()
      .entrySet()
      .stream()
      .filter(e -> OAuthConstants.FEATURE_TYPE.equals(e.getKey().getType()) &&
                   AwsCloudConnectorConstants.CLOUD_TYPE.equals(e.getKey().getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM)))
      .collect(Collectors.toList());

    for (Map.Entry<SProjectFeatureDescriptor, SProjectFeatureDescriptor> newAwsConnection : newAwsConnections) {
      final String sourceId = newAwsConnection.getKey().getId();
      final SProjectFeatureDescriptor copiedFeature = newAwsConnection.getValue();

      // Project Manager already holds a copy of the new project - it just isn't persisted yet
      final SProject newProject = myProjectManager.findProjectById(copiedFeature.getProjectId());
      if (newProject == null) {
        continue;
      }

      newProject.getOwnFeatures()
                .stream()
                .filter(feature -> sourceId.equals(feature.getParameters().get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM)))
                .forEach(feature -> {
                  final Map<String, String> newParams = new HashMap<>(feature.getParameters());
                  newParams.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, copiedFeature.getId());
                  newProject.updateFeature(feature.getId(), feature.getType(), newParams);
                });
    }
  }
}
