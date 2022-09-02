package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AwsConnectionsManager {

  /**
   * Will get AWS Connection ID from the properties, find corresponding AWS Connection
   * and return a {@link AwsConnectionDescriptor} with all properties like connectionId, providerType and all properties map.
   *
   * @param properties properties Map where should be the chosen AWS Connection ID parameter.
   * @return AwsConnectionBean data bean with all AWS Connection properties.
   * @throws LinkedAwsConnNotFoundException thrown when there is no corresponding {@link AwsCloudConnectorConstants#CHOSEN_AWS_CONN_ID_PARAM property} in the properties map,
   *                                        when there is no AWS Connection with specified ID or when the AWS Connection credentials creation failed.
   */
  @NotNull
  AwsConnectionDescriptor getLinkedAwsConnection(@NotNull final Map<String, String> properties) throws LinkedAwsConnNotFoundException;

  /**
   * Returns an AWS connection with specified ID, credentials will reference the Singleton object who is resopsible for credentials refreshing of this particular AWS Connection
   *
   * @param awsConnectionId - ID of the connection
   * @return {@link AwsConnectionDescriptor} containing information about connection that can be used to construct specific AWS clients or throws exception if no such connection can be found
   */
  @Nullable
  AwsConnectionDescriptor getAwsConnection(@NotNull final String awsConnectionId) throws AwsConnectionNotFoundException;

  /**
   * Returns an AWS connection with specified ID, credentials will be valid for specified period if time, {@link AwsConnectionsManager} will make a new request to the AWS API, credentials WILL NOT be refreshed automatically
   *
   * @param awsConnectionId - ID of the connection
   * @param sessionDuration - The duration of the session if you know the exact time for which these AWS Connection credentilas should be valid for
   * @return {@link AwsConnectionDescriptor} containing information about connection that can be used to construct specific AWS clients or null if no such connection can be found
   */
  @Nullable
  AwsConnectionDescriptor getAwsConnection(@NotNull final String awsConnectionId, @NotNull final String sessionDuration) throws AwsConnectorException;

  @Nullable
  AwsConnectionDescriptor getAwsConnectionFromBuildEnvVar(@NotNull final SBuild build) throws AwsBuildFeatureException;


  /**
   * Will get AWS Connection ID from the properties, find corresponding AWS Connection
   * and return a data bean with all properties like connectionId, providerType and all properties map.
   *
   * @param properties properties Map where should be the chosen AWS Connection ID parameter.
   * @param project    project which will be searched for the AWS Connection.
   * @return AwsConnectionBean data bean with all AWS Connection properties.
   * @throws LinkedAwsConnNotFoundException thrown when there is no corresponding {@link AwsCloudConnectorConstants#CHOSEN_AWS_CONN_ID_PARAM property} in the properties map,
   *                                        when there is no AWS Connection with specified ID or when the AWS Connection credentials creation failed.
   */
  @NotNull
  @Deprecated
  AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> properties, @NotNull final SProject project) throws LinkedAwsConnNotFoundException;

  /**
   * Returns a connection attached to specific project
   *
   * @param project              - project where connection is attached
   * @param awsConnectionId      - ID of the connection
   * @param connectionParameters - Additional parameters necessary to construct a connection(session duration etc.)
   * @return bean containing information about connection that can be used to construct specific AWS clients or null if no such connection can be found
   */
  @Nullable
  @Deprecated
  AwsConnectionBean getAwsConnection(@NotNull final SProject project, @NotNull String awsConnectionId, Map<String, String> connectionParameters);

  @Nullable
  @Deprecated
  AwsConnectionBean getEnvVarAwsConnectionForBuild(@NotNull final SBuild build) throws AwsBuildFeatureException;
}
