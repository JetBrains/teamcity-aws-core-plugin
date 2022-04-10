package jetbrains.buildServer.clouds.amazon.connector.errors.features;

import org.jetbrains.annotations.NotNull;

public class NoLinkedAwsConnectionException extends AwsBuildFeatureException {
  public NoLinkedAwsConnectionException(@NotNull final String message) {
    super(message);
  }
  public NoLinkedAwsConnectionException(@NotNull final Exception cause) {
    super(cause);
  }
}
