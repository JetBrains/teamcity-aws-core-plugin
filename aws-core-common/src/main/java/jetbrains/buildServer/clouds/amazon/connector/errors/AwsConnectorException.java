package jetbrains.buildServer.clouds.amazon.connector.errors;

import org.jetbrains.annotations.NotNull;

public class AwsConnectorException extends Exception {

  private final String myParameterName;

  public AwsConnectorException(@NotNull final String message) {
    super(message);
    myParameterName = "";
  }

  public AwsConnectorException(@NotNull final Exception cause) {
    super(cause);
    myParameterName = "";
  }

  public AwsConnectorException(@NotNull final String message, @NotNull final Exception cause) {
    super(message, cause);
    myParameterName = "";
  }

  public AwsConnectorException(@NotNull final String message, @NotNull final Exception cause, @NotNull final String parameterName) {
    super(message, cause);
    myParameterName = parameterName;
  }

  public AwsConnectorException(@NotNull final String message, @NotNull final String parameterName) {
    super(message);
    myParameterName = parameterName;
  }

  @NotNull
  public String getParameterName() {
    return myParameterName;
  }
}
