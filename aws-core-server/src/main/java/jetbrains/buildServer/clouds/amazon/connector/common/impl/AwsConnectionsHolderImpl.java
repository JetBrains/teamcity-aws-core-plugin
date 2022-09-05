package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionsHolderImpl implements AwsConnectionsHolder {

  public final String AWS_CONNECTIONS_IDX_STORAGE = "aws.connections.idx.storage";

  private final AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder;
  private final ProjectManager myProjectManager;

  private final ConcurrentHashMap<String, AwsConnectionDescriptor> awsConnections = new ConcurrentHashMap<>();

  public AwsConnectionsHolderImpl(@NotNull final AwsConnectionDescriptorBuilder awsConnectionDescriptorBuilder,
                                  @NotNull final ProjectManager projectManager) {
    myAwsConnectionDescriptorBuilder = awsConnectionDescriptorBuilder;
    myProjectManager = projectManager;
  }

  @Override
  public void addAwsConnection(@NotNull AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException {
    String awsConnectionId = awsConnectionDescriptor.getId();
    CustomDataStorage dataStorage = getDataStorage();
    Map<String, String> dataStorageValues = dataStorage.getValues();
    if (dataStorageValues != null && dataStorageValues.containsKey(awsConnectionId)) {
      throw new DuplicatedAwsConnectionIdException("AWS Connection with ID " + awsConnectionId + " already exists");
    }
    //TODO: TW-77164 add the refresher task
    awsConnections.put(awsConnectionId, awsConnectionDescriptor);
    dataStorage.putValue(awsConnectionId, awsConnectionDescriptor.getProjectId());
    dataStorage.flush();
  }

  @Override
  public void updateAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException {
    String connectionId = awsConnectionDescriptor.getId();
    CustomDataStorage dataStorage = getDataStorage();
    Map<String, String> dataStorageValues = dataStorage.getValues();
    if (dataStorageValues == null || !dataStorageValues.containsKey(connectionId)) {
      addAwsConnection(awsConnectionDescriptor);
    } else {
      //TODO: TW-77164 update the refresher task
      awsConnections.put(connectionId, awsConnectionDescriptor);
    }
  }

  @Override
  public void removeAwsConnection(@NotNull final String awsConnectionId) {
    awsConnections.remove(awsConnectionId);
    removeAwsConnectionFromDataStorage(awsConnectionId);
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor findAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectionNotFoundException {
    AwsConnectionDescriptor awsConnectionDescriptor = awsConnections.get(awsConnectionId);
    if (awsConnectionDescriptor == null) {
      Loggers.CLOUD.debug(
        String.format("Trying to construct AWS Connection for the first time as it has not been added to the map yet, connection ID: '%s'", awsConnectionId));
      return getConnectionViaOwnerProject(awsConnectionId);
    }
    return awsConnectionDescriptor;
  }

  @Override
  public void clear() {
    awsConnections.clear();
  }

  @Override
  public void rebuildAllConnectionsForProject(@NotNull String projectId) {
    SProject updatedProject = myProjectManager.findProjectById(projectId);
    if (updatedProject == null) {
      Loggers.CLOUD.debug(
        String.format("Could not find the reloaded project with ID: '%s'", projectId));
      return;
    }

    for (SProjectFeatureDescriptor projectFeature : updatedProject.getOwnFeaturesOfType(OAuthConstants.FEATURE_TYPE)) {
      if (AwsConnectionProvider.TYPE.equals(projectFeature.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM))){
        try {
          updateAwsConnection(
            buildAwsConnectionDescriptor(projectFeature.getId(), projectId)
          );
        } catch (AwsConnectorException e) {
          Loggers.CLOUD.warnAndDebugDetails(
            String.format("Failed to reload AWS Connection with ID '%s' in Project with ID: '%s', reason: <%s>", projectFeature.getId(), projectId, e.getMessage()), e);
        }
      }
    }
  }

  @Override
  public void removeAllConnectionsForProject(@NotNull SProject project) {
    Map<String, String> values = getDataStorage().getValues();
    if (values != null) {
      values.forEach((connectionId, ownerProjectId) -> {
        if (ownerProjectId.equals(project.getProjectId())) {
          removeAwsConnection(connectionId);
        }
      });
    }
  }


  @NotNull
  private AwsConnectionDescriptor getConnectionViaOwnerProject(@NotNull final String awsConnectionId) throws AwsConnectionNotFoundException {
    CustomDataStorage dataStorage = getDataStorage();
    Map<String, String> dataStorageValues = dataStorage.getValues();
    if (dataStorageValues == null || !dataStorageValues.containsKey(awsConnectionId)) {
      throw new AwsConnectionNotFoundException("There is no AWS Connection with ID: " + awsConnectionId);
    }
    String projectIdWhereToLookForConnection = dataStorageValues.get(awsConnectionId);
    try {
      Loggers.CLOUD.debug(
        String.format("Building AWS Connection with ID: '%s', from the Project with ID: '%s'", awsConnectionId, projectIdWhereToLookForConnection));
      return buildAwsConnectionDescriptor(awsConnectionId, projectIdWhereToLookForConnection);

    } catch (AwsConnectorException e) {
      throw new AwsConnectionNotFoundException(e.getMessage());
    }
  }

  @NotNull
  private AwsConnectionDescriptor buildAwsConnectionDescriptor(@NotNull final String connectionId, @Nullable final String projectId) throws AwsConnectorException {
    if (projectId == null) {
      throw new AwsConnectorException("The connection with ID: " + connectionId + " is not assosiated with any project (Project ID is null)");
    }
    SProject project = myProjectManager.findProjectById(projectId);
    if (project == null) {
      throw new AwsConnectorException("The project with ID: " + projectId + " does not exist");
    }
    return myAwsConnectionDescriptorBuilder.findInProjectAndBuild(project, connectionId);
  }

  private void removeAwsConnectionFromDataStorage(@NotNull final String awsConnectionId) {
    CustomDataStorage storage = getDataStorage();
    Map<String, String> values = storage.getValues();
    if (values != null) {
      Set<String> removedKey = new HashSet<>();
      removedKey.add(awsConnectionId);
      storage.updateValues(Collections.emptyMap(), removedKey);
    }
    storage.flush();
  }

  @NotNull
  private CustomDataStorage getDataStorage() {
    return myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_IDX_STORAGE);
  }
}
