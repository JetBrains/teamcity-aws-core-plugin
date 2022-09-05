package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionsHolder {

  void addAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException;

  void updateAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException;

  void removeAwsConnection(@NotNull final String awsConnectionId);

  @NotNull
  AwsConnectionDescriptor findAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectionNotFoundException;

  void clear();

  void rebuildAllConnectionsForProject(@NotNull final String projectId);

  void removeAllConnectionsForProject(@NotNull final SProject project);
}
