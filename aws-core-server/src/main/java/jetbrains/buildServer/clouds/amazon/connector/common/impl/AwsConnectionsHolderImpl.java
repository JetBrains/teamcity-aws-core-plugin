package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
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
    String connecionOwnerProject = getDataStorageValue(awsConnectionId);
    if (connecionOwnerProject != null) {
      throw new DuplicatedAwsConnectionIdException("AWS Connection with ID " + awsConnectionId + " already exists");
    }
    //TODO: TW-77164 add the refresher task
    awsConnections.put(awsConnectionId, awsConnectionDescriptor);
    putDataStorageValue(awsConnectionId, awsConnectionDescriptor.getProjectId());
  }

  @Override
  public void updateAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException {
    String connectionId = awsConnectionDescriptor.getId();
    String connecionOwnerProject = getDataStorageValue(connectionId);
    if (connecionOwnerProject == null) {
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
  public AwsConnectionDescriptor getAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectorException {
    AwsConnectionDescriptor awsConnectionDescriptor = awsConnections.get(awsConnectionId);
    if (awsConnectionDescriptor == null) {
      AwsConnectionsLogger.connectionRequested(awsConnectionId);
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
      AwsConnectionsLogger.projectNotFound(projectId);
      return;
    }

    for (SProjectFeatureDescriptor projectFeature : updatedProject.getOwnFeaturesOfType(OAuthConstants.FEATURE_TYPE)) {
      if (AwsConnectionProvider.TYPE.equals(projectFeature.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM))) {
        try {
          updateAwsConnection(
            buildAwsConnectionDescriptor(projectFeature.getId(), projectId)
          );
        } catch (AwsConnectorException e) {
          new AwsConnectionsLogger(updatedProject)
            .failedToBuild(projectFeature.getId(), e);
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
  private AwsConnectionDescriptor getConnectionViaOwnerProject(@NotNull final String awsConnectionId) throws AwsConnectorException {
    String projectIdWhereToLookForConnection = getDataStorageValue(awsConnectionId);
    if (projectIdWhereToLookForConnection == null) {
      throw new AwsConnectionNotFoundException("There is no AWS Connection with ID: " + awsConnectionId);
    }
    AwsConnectionsLogger.connectionRequested(awsConnectionId, projectIdWhereToLookForConnection);
    return buildAwsConnectionDescriptor(awsConnectionId, projectIdWhereToLookForConnection);
  }

  @NotNull
  private AwsConnectionDescriptor buildAwsConnectionDescriptor(@NotNull final String connectionId, @Nullable final String projectId) throws AwsConnectorException {
    SProject project = myProjectManager.findProjectById(projectId);
    if (project == null) {
      throw new AwsConnectorException("The project with ID: " + projectId + " does not exist");
    }
    return myAwsConnectionDescriptorBuilder.findInProjectAndBuild(project, connectionId);
  }

  private void removeAwsConnectionFromDataStorage(@NotNull final String awsConnectionId) {
    CustomDataStorage storage = getDataStorage();
    storage.updateValues(Collections.emptyMap(), Collections.singleton(awsConnectionId));
    storage.flush();
  }

  @Nullable
  private String getDataStorageValue(@NotNull final String key) {
    CustomDataStorage dataStorage = getDataStorage();
    Map<String, String> dataStorageValues = dataStorage.getValues();
    if (dataStorageValues == null || !dataStorageValues.containsKey(key)) {
      return null;
    }
    return dataStorageValues.get(key);
  }

  private void putDataStorageValue(@NotNull final String key, @NotNull final String value) {
    CustomDataStorage dataStorage = getDataStorage();
    dataStorage.putValue(key, value);
    dataStorage.flush();
  }

  @NotNull
  private CustomDataStorage getDataStorage() {
    return myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_IDX_STORAGE);
  }
}
