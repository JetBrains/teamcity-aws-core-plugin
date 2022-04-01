package jetbrains.buildServer.clouds.amazon.connector.connectionTesting;

import com.amazonaws.AmazonClientException;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl.AwsTestConnectionResult;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionTester {
  @NotNull
  AwsTestConnectionResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AmazonClientException;

  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties);
}
