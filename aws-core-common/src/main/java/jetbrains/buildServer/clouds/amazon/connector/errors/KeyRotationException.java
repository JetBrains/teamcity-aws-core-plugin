package jetbrains.buildServer.clouds.amazon.connector.errors;

import org.jetbrains.annotations.NotNull;

public class KeyRotationException extends AwsConnectorException {
  public KeyRotationException(@NotNull final String message) {
    super(message);
  }
  public KeyRotationException(@NotNull final String message, @NotNull final Exception cause) {
    super(message, cause);
  }
  public KeyRotationException(@NotNull final Exception cause) {
    super(cause);
  }
}
