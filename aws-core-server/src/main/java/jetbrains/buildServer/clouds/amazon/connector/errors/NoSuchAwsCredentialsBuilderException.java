package jetbrains.buildServer.clouds.amazon.connector.errors;

import org.jetbrains.annotations.NotNull;

public class NoSuchAwsCredentialsBuilderException extends AwsConnectorException {
  public NoSuchAwsCredentialsBuilderException(@NotNull final String message) {
    super(message);
  }
}
