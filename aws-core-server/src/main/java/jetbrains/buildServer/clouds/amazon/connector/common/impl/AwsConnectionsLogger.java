package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionsLogger {

  private final Logger LOG = Logger.getInstance(AwsConnectionsLogger.class.getName());

  private final SProject myProject;

  public AwsConnectionsLogger(@NotNull final SProject project) {
    myProject = project;
  }

  public static void projectNotFound(@NotNull final String projectId) {
    Loggers.CLOUD.debug(String.format(
      "Could not find a project with ID: '%s'",
      projectId
    ));
  }

  public static void connectionBuildRequested(@NotNull final String awsConnectionId, @NotNull final String projectId) {
    Loggers.CLOUD.debug(String.format(
      "Building AWS Connection with ID: '%s', from the Project with ID: '%s'",
      awsConnectionId,
      projectId
    ));
  }

  public static void duplicatedAwsConnectionExistsOnTheServer(@NotNull final String awsConnectionId,
                                                              @Nullable final SProject originalConnectionProject,
                                                              @Nullable final SProject duplicatedConnectionProject) {
    String originalConnectionProjectId = "";
    if (originalConnectionProject != null) {
      originalConnectionProjectId = originalConnectionProject.getExternalId();
    }
    String duplicatedConnectionProjectId = "";
    if (duplicatedConnectionProject != null) {
      duplicatedConnectionProjectId = duplicatedConnectionProject.getExternalId();
    }
    Loggers.CLOUD.error(String.format(
      "AWS Connection with ID <%s> in the project <%s> is broken, because another AWS Connection with the same ID already exists in the project <%s>",
      awsConnectionId,
      duplicatedConnectionProjectId,
      originalConnectionProjectId
    ));
  }

  public void connectionAdded(@NotNull final String awsConnectionId) {
    LOG.debug(String.format(
      "Added AWS Connection '%s' in the Project with ID: '%s'",
      awsConnectionId,
      myProject.getExternalId()
    ));
  }

  public void failedToAdd(@NotNull final String awsConnectionId, @NotNull final Exception cause) {
    LOG.warnAndDebugDetails(String.format(
      "Can not add AWS Connection '%s' in the Project with ID: '%s', reason: <%s>",
      awsConnectionId,
      myProject.getExternalId(),
      cause.getMessage()
    ), cause);
  }

  public void connectionUpdated(@NotNull final String connectionId) {
    Loggers.CLOUD.debug(String.format(
      "Updated AWS Connection '%s' in the Project with ID: '%s'",
      connectionId,
      myProject.getExternalId()
    ));
  }

  public void failedToUpdate(@NotNull final String connectionId, @NotNull final Exception cause) {
    Loggers.CLOUD.warnAndDebugDetails(String.format(
      "Can not update AWS Connection '%s' in the Project with ID: '%s', reason: <%s>",
      connectionId,
      myProject.getExternalId(),
      cause.getMessage()
    ), cause);
  }

  public void connectionRemoved(@NotNull final String awsConnectionId) {
    Loggers.CLOUD.debug(String.format(
      "Removed AWS Connection '%s' in the Project with ID: '%s'",
      awsConnectionId,
      myProject.getExternalId()
    ));
  }

  public void failedToBuild(@NotNull final String awsConnectionId, @NotNull final Exception cause) {
    Loggers.CLOUD.warnAndDebugDetails(String.format(
      "Failed to reload AWS Connection with ID '%s' in Project with ID: '%s', reason: <%s>",
      awsConnectionId,
      myProject.getExternalId(),
      cause.getMessage()
    ), cause);
  }
}
