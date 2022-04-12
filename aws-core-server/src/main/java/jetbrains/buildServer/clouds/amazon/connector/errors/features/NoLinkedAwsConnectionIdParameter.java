package jetbrains.buildServer.clouds.amazon.connector.errors.features;

import org.jetbrains.annotations.NotNull;

public class NoLinkedAwsConnectionIdParameter extends AwsBuildFeatureException {
  public NoLinkedAwsConnectionIdParameter(@NotNull final String message) {
    super(message);
  }
  public NoLinkedAwsConnectionIdParameter(@NotNull final Exception cause) {
    super(cause);
  }
}
