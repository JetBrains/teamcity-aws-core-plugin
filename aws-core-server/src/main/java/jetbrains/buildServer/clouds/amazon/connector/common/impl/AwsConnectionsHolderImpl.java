package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME;

public class AwsConnectionsHolderImpl implements AwsConnectionsHolder {

  public static final String AWS_CONNECTIONS_IDX_STORAGE = "aws.connections.idx.storage";

  private final AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder;
  private final ProjectManager myProjectManager;
  private final AwsCredentialsRefresheringManager myAwsCredentialsRefresheringManager;

  private final ConcurrentHashMap<String, AwsConnectionDescriptor> awsConnections = new ConcurrentHashMap<>();

  public AwsConnectionsHolderImpl(@NotNull final AwsConnectionDescriptorBuilder awsConnectionDescriptorBuilder,
                                  @NotNull final ProjectManager projectManager,
                                  @NotNull final AwsCredentialsRefresheringManager awsCredentialsRefresheringManager) {
    myAwsConnectionDescriptorBuilder = awsConnectionDescriptorBuilder;
    myProjectManager = projectManager;
    myAwsCredentialsRefresheringManager = awsCredentialsRefresheringManager;
  }

  @Override
  public void addAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    String awsConnectionId = awsConnectionDescriptor.getId();
    initAwsConnection(awsConnectionDescriptor);
    putDataStorageValue(awsConnectionId, awsConnectionDescriptor.getProjectId());
  }

  @Override
  public void updateAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException {
    String connectionId = awsConnectionDescriptor.getId();
    String connectionOwnerProjectId = getDataStorageValue(connectionId);
    if (connectionOwnerProjectId == null) {
      addAwsConnection(awsConnectionDescriptor);
    } else if (!connectionOwnerProjectId.equals(awsConnectionDescriptor.getProjectId())){
      SProject originalConnectionProject = myProjectManager.findProjectById(connectionOwnerProjectId);
      SProject duplicatedConnectionProject = myProjectManager.findProjectById(awsConnectionDescriptor.getProjectId());
      AwsConnectionsLogger.duplicatedAwsConnectionExistsOnTheServer(connectionId, originalConnectionProject, duplicatedConnectionProject);
    } else {
      initAwsConnection(awsConnectionDescriptor);
    }
  }

  @Override
  public void removeAwsConnection(@NotNull final String awsConnectionId) {
    myAwsCredentialsRefresheringManager.stopCredentialsRefreshingtask(awsConnectionId);
    awsConnections.remove(awsConnectionId);
    removeAwsConnectionFromDataStorage(awsConnectionId);
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor getAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectorException {
    AwsConnectionDescriptor awsConnectionDescriptor = awsConnections.get(awsConnectionId);
    if (awsConnectionDescriptor == null) {
      awsConnectionDescriptor = buildConnectionFromOwnerProject(awsConnectionId);
      initAwsConnection(awsConnectionDescriptor);
    } else if (isDefaultCredsProviderChainType(awsConnectionDescriptor) &&
               ! TeamCityProperties.getBoolean(DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME)) {
      removeAwsConnection(awsConnectionId);
      throw new AwsConnectorException("Default Credentials Provider is disabled on this server, please, contact the server Administrator");
    }

    return awsConnectionDescriptor;
  }

  @Override
  public void clear() {
    myAwsCredentialsRefresheringManager.dispose();
    awsConnections.clear();
  }

  @Override
  public void rebuildAllConnectionsForProject(@NotNull String projectId) {
    SProject updatedProject = myProjectManager.findProjectById(projectId);
    if (updatedProject == null) {
      AwsConnectionsLogger.projectNotFound(projectId);
      return;
    }

    Collection<SProjectFeatureDescriptor> updatedAwsConnections = getAwsConnectionFeatures(updatedProject);
    freeChangedIds(updatedAwsConnections, projectId);

    AwsConnectionsLogger awsConnectionsLogger = new AwsConnectionsLogger(updatedProject);
    for (SProjectFeatureDescriptor connectionFeature : updatedAwsConnections) {
      awsConnectionsLogger.rebuildAwsConnectionOnProjectRestore(connectionFeature.getId());
      try {
        updateAwsConnection(
          buildAwsConnectionDescriptor(connectionFeature.getId(), projectId)
        );
      } catch (Exception e) {
        awsConnectionsLogger.failedToBuild(connectionFeature.getId(), e);
      }
    }
  }

  @Override
  public void removeAllConnectionsForProject(@NotNull SProject project) {
    for (SProjectFeatureDescriptor connectionFeature : getAwsConnectionFeatures(project)) {
      removeAwsConnection(connectionFeature.getId());
    }
  }

  @Override
  public boolean isUniqueAwsConnectionId(@NotNull final String awsConnectionId) {
    return getDataStorageValue(awsConnectionId) == null;
  }


  @NotNull
  private AwsConnectionDescriptor buildConnectionFromOwnerProject(@NotNull final String awsConnectionId) throws AwsConnectorException {
    String projectIdWhereToLookForConnection = getDataStorageValue(awsConnectionId);
    if (projectIdWhereToLookForConnection == null) {
      throw new AwsConnectionNotFoundException(awsConnectionId);
    }
    AwsConnectionsLogger.connectionBuildRequested(awsConnectionId, projectIdWhereToLookForConnection);
    return buildAwsConnectionDescriptor(awsConnectionId, projectIdWhereToLookForConnection);
  }

  @NotNull
  private AwsConnectionDescriptor buildAwsConnectionDescriptor(@NotNull final String connectionId, @Nullable final String projectId) throws AwsConnectorException {
    SProject project = myProjectManager.findProjectById(projectId);
    if (project == null) {
      throw new AwsConnectorException("The project with ID: " + projectId + " does not exist");
    }
    return myAwsConnectionDescriptorBuilder.buildFromProject(project, connectionId);
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

  public void putDataStorageValue(@NotNull final String key, @NotNull final String value) {
    CustomDataStorage dataStorage = getDataStorage();
    dataStorage.putValue(key, value);
    dataStorage.flush();
  }

  @NotNull
  private CustomDataStorage getDataStorage() {
    return myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_IDX_STORAGE);
  }

  private Collection<SProjectFeatureDescriptor> getAwsConnectionFeatures(@NotNull final SProject project) {
    return project
      .getOwnFeaturesOfType(OAuthConstants.FEATURE_TYPE)
      .stream()
      .filter(featureDescriptor -> {
        String oauthTypeParam = featureDescriptor.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
        return AwsConnectionProvider.TYPE.equals(oauthTypeParam);
      })
      .collect(Collectors.toList());
  }

  private void initAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    myAwsCredentialsRefresheringManager.scheduleCredentialRefreshingTask(awsConnectionDescriptor);
    awsConnections.put(awsConnectionDescriptor.getId(), awsConnectionDescriptor);
  }

  private void freeChangedIds(@NotNull final Collection<SProjectFeatureDescriptor> updatedAwsConnections, @NotNull final String projectId) {
    Map<String, String> dataStorageValues = getDataStorage().getValues();
    if (dataStorageValues != null) {
      ArrayList<String> previousOwnedByProjectAwsConnections = new ArrayList<>();
      dataStorageValues.forEach((connectionId, projectOwnerId) -> {
        if (projectOwnerId.equals(projectId)) {
          previousOwnedByProjectAwsConnections.add(connectionId);
        }
      });

      previousOwnedByProjectAwsConnections
        .removeAll(
          updatedAwsConnections
            .stream()
            .map(SProjectFeatureDescriptor::getId)
            .collect(Collectors.toList())
        );
      for (String removedAwsConnectionId: previousOwnedByProjectAwsConnections) {
        removeAwsConnectionFromDataStorage(removedAwsConnectionId);
      }
    }
  }

  private boolean isDefaultCredsProviderChainType(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    return AwsCloudConnectorConstants.DEFAULT_PROVIDER_CREDENTIALS_TYPE
      .equals(
        awsConnectionDescriptor
          .getParameters()
          .get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM)
      );
  }
}
