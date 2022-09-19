package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType.externalId;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.isAwsConnectionFeature;

public class AwsExternalIdsManagerImpl extends ProjectsModelListenerAdapter implements AwsExternalIdsManager {

  private static final Logger LOG = Logger.getInstance(AwsExternalIdsManagerImpl.class.getName());
  private final static String AWS_CONNECTIONS_EXTERNAL_IDX_STORAGE = "aws.connections.external.idx.storage";
  private final ProjectManager myProjectManager;

  public AwsExternalIdsManagerImpl(@NotNull final EventDispatcher<ProjectsModelListener> buildServerEventDispatcher,
                                   @NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
    buildServerEventDispatcher.addListener(this);
  }

  @Override
  @NotNull
  public String getOrGenerateAwsConnectionExternalId(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    SProject project = getProjectById(featureDescriptor.getProjectId());
    String awsConnectionId = featureDescriptor.getId();

    final CustomDataStorage storage = getDataStorage(project);
    String storedExternalId = storage.getValue(awsConnectionId);
    
    if (storedExternalId != null) {
      return storedExternalId;
    }

    String newExternalId = generateExternalId();
    storage.putValue(awsConnectionId, newExternalId);
    storage.flush();
    LOG.debug(String.format("Added new External ID for AWS Connection '%s' in the Project with ID: '%s'", awsConnectionId, project.getExternalId()));
    return newExternalId;
  }

  @Override
  public void projectFeatureRemoved(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor projectFeature) {
    if (!isAwsConnectionFeature(projectFeature)) {
      return;
    }

    removeExternalId(project, projectFeature.getId());
    LOG.debug(String.format("Removed External ID for AWS Connection '%s' in the Project with ID: '%s'", projectFeature.getId(), project.getExternalId()));
  }

  @NotNull
  private CustomDataStorage getDataStorage(@NotNull final SProject project) {
    return project.getCustomDataStorage(AWS_CONNECTIONS_EXTERNAL_IDX_STORAGE);
  }

  @NotNull
  private SProject getProjectById(@Nullable final String projectId) throws AwsConnectorException {
    SProject project = myProjectManager.findProjectById(projectId);
    if (project == null) {
      String errorMessage = "Project with ID: " + projectId + " was not found";
      LOG.debug("Failed to get External ID to assume IAM Role: " + errorMessage);
      throw new AwsConnectorException(errorMessage);
    }
    return project;
  }

  @NotNull
  private String generateExternalId() {
    return UUID.randomUUID().toString();
  }

  private void removeExternalId(@NotNull final SProject project, @NotNull final String connectionId) {
    final CustomDataStorage storage = getDataStorage(project);
    storage.putValue(connectionId, null);
  }
}