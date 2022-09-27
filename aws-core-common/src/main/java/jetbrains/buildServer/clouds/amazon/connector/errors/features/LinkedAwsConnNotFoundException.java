package jetbrains.buildServer.clouds.amazon.connector.errors.features;

import org.jetbrains.annotations.NotNull;

public class LinkedAwsConnNotFoundException extends AwsBuildFeatureException {
  public LinkedAwsConnNotFoundException(@NotNull final String message) {
    super(message);
  }
  public LinkedAwsConnNotFoundException(@NotNull final Exception cause) {
    super(cause);
  }

  public LinkedAwsConnNotFoundException(@NotNull final String message, @NotNull final Exception cause){
    super(message, cause);
  }
}
