package jetbrains.buildServer.clouds.amazon.connector.errors.features;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import org.jetbrains.annotations.NotNull;

public class AwsBuildFeatureException extends AwsConnectorException {
  public AwsBuildFeatureException(@NotNull final String message) {
    super(message);
  }
  public AwsBuildFeatureException(@NotNull final Exception cause) {
    super(cause);
  }

  public AwsBuildFeatureException(@NotNull final String message, @NotNull final Exception cause) {
    super(message, cause);
  }
}
