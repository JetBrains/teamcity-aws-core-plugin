package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

/**
 * All AWS Connections-related management logic is in the {@link jetbrains.buildServer.serverSide.connections.aws.AwsConnectionCredentialsFactory}.
 * @deprecated Use {@link jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager} to retrieve credentials.
 */
@Deprecated
public interface AwsConnectionsHolder {

  void addAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException;

  void updateAwsConnection(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) throws DuplicatedAwsConnectionIdException;

  void removeAwsConnection(@NotNull final String awsConnectionId);

  @NotNull
  AwsConnectionDescriptor getAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectorException;

  void clear();

  void rebuildAllConnectionsForProject(@NotNull final String projectId);

  void removeAllConnectionsForProject(@NotNull final SProject project);

  boolean isUniqueAwsConnectionId(@NotNull final String awsConnectionId);

  void putDataStorageValue(@NotNull final String awsConnectionId, @NotNull final String projectOwnerid);
}
