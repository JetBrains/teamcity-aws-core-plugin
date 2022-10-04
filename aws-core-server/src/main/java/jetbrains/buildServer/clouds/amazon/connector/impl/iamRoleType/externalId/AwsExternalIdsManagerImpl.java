package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType.externalId;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsExternalIdsManager;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsExternalIdsManagerImpl implements AwsExternalIdsManager {

  private static final Logger LOG = Logger.getInstance(AwsExternalIdsManagerImpl.class.getName());
  private final ProjectManager myProjectManager;

  public AwsExternalIdsManagerImpl(@NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  @Override
  @NotNull
  public String getAwsConnectionExternalId(@NotNull final String awsConnectionId, @NotNull final String projectId) throws AwsConnectorException {
    String awsConnectionExternalId = generateExternalId(awsConnectionId, getProjectById(projectId).getExternalId());
    LOG.debug(String.format("Returning External ID for AWS Connection: %s", awsConnectionExternalId));
    return awsConnectionExternalId;
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
  private String generateExternalId(@NotNull final String projectExternalId, @NotNull final String connectionId) {
    return String.format("%s-%s", projectExternalId, connectionId);
  }
}