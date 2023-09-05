
package jetbrains.buildServer.serverSide.connections.aws;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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

      updateBuildTypeSettings(newProject::getOwnBuildTypes, sourceId, copiedFeature);
      updateBuildTypeSettings(newProject::getOwnBuildTypeTemplates, sourceId, copiedFeature);

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

  private void updateBuildTypeSettings(Supplier<List<?>> supplier, String sourceId, SProjectFeatureDescriptor copiedFeature) {
    List<BuildTypeSettings> bts = supplier.get()
                                          .stream()
                                          .map(bt -> (BuildTypeSettings)bt)
                                          .collect(Collectors.toList());
    updateBuildFeatures(sourceId, copiedFeature, bts);
  }

  private void updateBuildFeatures(String sourceId, SProjectFeatureDescriptor copiedFeature, List<BuildTypeSettings> btSettingsList) {
    for (BuildTypeSettings bts : btSettingsList) {
      Collection<SBuildFeatureDescriptor> features = bts.getBuildFeatures();

      for (SBuildFeatureDescriptor feature : features) {
        if (sourceId.equals(feature.getParameters().get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM))) {
          final Map<String, String> newParams = new HashMap<>(feature.getParameters());
          newParams.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, copiedFeature.getId());
          bts.updateBuildFeature(feature.getId(), feature.getType(), newParams);
        }
      }
    }
  }

}
