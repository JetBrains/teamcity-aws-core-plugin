package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

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

  public void connectionUpdated(@NotNull final String previousConnectionId, @NotNull final String updatedConnectionId) {
    Loggers.CLOUD.debug(String.format(
      "Updated AWS Connection '%s' in the Project with ID: '%s', previous connection ID: '%s'",
      updatedConnectionId,
      myProject.getExternalId(),
      previousConnectionId
    ));
  }

  public void failedToUpdate(@NotNull final String previousConnectionId, @NotNull final String updatedConnectionId, @NotNull final Exception cause) {
    Loggers.CLOUD.warnAndDebugDetails(String.format(
      "Can not update AWS Connection '%s' in the Project with ID: '%s', previous connection ID: '%s', reason: <%s>",
      updatedConnectionId,
      myProject.getExternalId(),
      previousConnectionId,
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
