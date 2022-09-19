package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

public interface AwsExternalIdsManager {
  @NotNull
  String getOrGenerateAwsConnectionExternalId(@NotNull final SProjectFeatureDescriptor awsConnectionDescriptor) throws AwsConnectorException;
}
