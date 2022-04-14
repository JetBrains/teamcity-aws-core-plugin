package jetbrains.buildServer.clouds.amazon.connector.errors.features;

import org.jetbrains.annotations.NotNull;

public class AwsBuildFeatureException extends Exception {
  public AwsBuildFeatureException(@NotNull final String message) {
    super(message);
  }
  public AwsBuildFeatureException(@NotNull final Exception cause) {
    super(cause);
  }
}
