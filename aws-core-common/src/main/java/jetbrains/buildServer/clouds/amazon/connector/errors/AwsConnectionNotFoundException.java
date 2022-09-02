package jetbrains.buildServer.clouds.amazon.connector.errors;

import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionNotFoundException extends AwsBuildFeatureException {
  public AwsConnectionNotFoundException(@NotNull final String message) {
    super(message);
  }
  public AwsConnectionNotFoundException(@NotNull final Exception cause) {
    super(cause);
  }
}
