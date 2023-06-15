package jetbrains.buildServer.clouds.amazon.connector;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Searches for the Connection which is linked to some ProjectFeature
 *
 * @since 2023.11
 */
public interface LinkedAwsConnectionProvider {

  /**
   * Validates all linked AWS Connection properties, finds corresponding AWS Connection
   * and returns its {@link ConnectionCredentials}.
   *
   * @param featureWithLinkedConnection A feature with linked AWS Connection in its properties.
   * @return {@link ConnectionCredentials} containing all properties with credentials. For exact property names, please, check corresponding {@link jetbrains.buildServer.serverSide.connections.ConnectionProvider}.
   * @throws ConnectionCredentialsException thrown when there is no linked AWS Connection ID in the feature properties map,
   *                                        when there is no AWS Connection with specified ID or when something is wrong with the AWS Connection credentials fetching or invalid additional linked AWS Connection properties, like session duration.
   */
  @NotNull
  ConnectionCredentials getLinkedConnectionCredentials(@NotNull final SProjectFeatureDescriptor featureWithLinkedConnection) throws ConnectionCredentialsException;

  /**
   * Validates all linked AWS Connection properties and finds specified linked AWS Connection.
   * Will <b>not</b> fetch its {@link ConnectionCredentials}.
   *
   * @param project           A project where to look for the linked AWS Connection.
   * @param featureProperties Properties with specified linked AWS Connection ID.
   * @return {@link ConnectionDescriptor} containing all information of requested AWS Connection without requesting its credentials.
   * @throws ConnectionCredentialsException thrown when there is no linked AWS Connection ID in the feature properties map,
   *                                        when there is no AWS Connection with specified ID or when something is wrong with additional linked AWS Connection properties, like session duration.
   */
  @NotNull
  ConnectionDescriptor getLinkedConnectionFromParameters(@NotNull final SProject project, @NotNull final Map<String, String> featureProperties) throws ConnectionCredentialsException;

  /**
   * Checks if the build has AWS Connections to inject and if so, validates all linked AWS Connection properties, finds corresponding AWS Connection
   * and returns its {@link ConnectionCredentials}.
   *
   * @param build A build where to look for the linked AWS Connection.
   * @return {@link ConnectionCredentials} containing all properties with credentials or empty if the build does not have any linked AWS Connections to inject. For exact property names, please, check corresponding {@link jetbrains.buildServer.serverSide.connections.ConnectionProvider}. Right now only one AWS Credentials BuildFeature is processed. See: TW-75618 Add support for several AWS Connections exposing.
   * @throws ConnectionCredentialsException thrown when there is no linked AWS Connection ID in the feature properties map,
   *                                        when there is no AWS Connection with specified ID or when something is wrong with linked AWS Connection credentials fetching or invalid additional linked AWS Connection properties, like session duration.
   */
  @NotNull
  List<ConnectionCredentials> getConnectionCredentialsFromBuild(@NotNull final SBuild build) throws ConnectionCredentialsException;
}
