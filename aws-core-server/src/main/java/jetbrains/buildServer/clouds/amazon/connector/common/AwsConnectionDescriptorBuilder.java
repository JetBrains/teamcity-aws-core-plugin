package jetbrains.buildServer.clouds.amazon.connector.common;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionDescriptorBuilder {
  @NotNull
  AwsConnectionDescriptor findInProjectAndBuild(@NotNull final SProject project, @NotNull final String awsFeatureConnectionId) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor fromFeatureDescriptor(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull final String sessionDuration) throws AwsConnectorException;

  @NotNull
  @Deprecated
  AwsConnectionBean awsConnBeanFromDescriptor(@NotNull final AwsConnectionDescriptor connectionDescriptor, @NotNull final Map<String, String> connectionParameters) throws AwsConnectorException;
}
