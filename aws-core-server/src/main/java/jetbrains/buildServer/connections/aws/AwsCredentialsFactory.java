package jetbrains.buildServer.connections.aws;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import org.jetbrains.annotations.NotNull;

public interface AwsCredentialsFactory {
  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);
}
