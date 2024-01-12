

package jetbrains.buildServer.clouds.amazon.connector.common;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * All AWS Connections-related management logic is in the {@link AwsConnectionCredentialsFactory}.
 * @deprecated Use {@link jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager} to retrieve credentials.
 */
@Deprecated
public interface AwsConnectionDescriptorBuilder {
  @NotNull
  AwsConnectionDescriptor buildFromProject(@NotNull final SProject project, @NotNull final String awsFeatureConnectionId) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor fromFeatureDescriptor(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull final String sessionDuration) throws AwsConnectorException;

  @NotNull
  @Deprecated
  AwsConnectionBean awsConnBeanFromDescriptor(@NotNull final AwsConnectionDescriptor connectionDescriptor, @NotNull final Map<String, String> connectionParameters) throws AwsConnectorException;
}