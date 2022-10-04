package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import org.jetbrains.annotations.NotNull;

public interface AwsExternalIdsManager {
  @NotNull
  String getAwsConnectionExternalId(@NotNull final String awsConnectionId, @NotNull final String projectExternalId) throws AwsConnectorException;
}
