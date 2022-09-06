package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionEventsLogger {

  private final Logger LOG = Logger.getInstance(AwsConnectionEventsLogger.class.getName());

  private final SProject myProject;

  public AwsConnectionEventsLogger(@NotNull final SProject project) {
    myProject = project;
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
      "Can not update AWS Connection '%s' in the Project with ID: '%s', reason: <%s>, previous connection ID: '%s'",
      updatedConnectionId,
      myProject.getExternalId(),
      cause.getMessage(),
      previousConnectionId
    ), cause);
  }

  public void connectionRemoved(@NotNull final String awsConnectionId) {
    Loggers.CLOUD.debug(String.format(
      "Removed AWS Connection '%s' in the Project with ID: '%s'",
      awsConnectionId,
      myProject.getExternalId()
    ));
  }
}
