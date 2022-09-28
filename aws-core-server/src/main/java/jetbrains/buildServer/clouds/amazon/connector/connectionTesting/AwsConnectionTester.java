package jetbrains.buildServer.clouds.amazon.connector.connectionTesting;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl.AwsTestConnectionResult;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionTester {
  @NotNull
  AwsTestConnectionResult testConnection(@NotNull final ProjectFeatureDescriptorImpl connectionFeature) throws AwsConnectorException;

  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties);
}
