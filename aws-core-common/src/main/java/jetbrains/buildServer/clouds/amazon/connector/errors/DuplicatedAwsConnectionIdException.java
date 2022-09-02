package jetbrains.buildServer.clouds.amazon.connector.errors;

import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import org.jetbrains.annotations.NotNull;

public class DuplicatedAwsConnectionIdException extends AwsBuildFeatureException {
  public DuplicatedAwsConnectionIdException(@NotNull final String message) {
    super(message);
  }
  public DuplicatedAwsConnectionIdException(@NotNull final Exception cause) {
    super(cause);
  }
}
