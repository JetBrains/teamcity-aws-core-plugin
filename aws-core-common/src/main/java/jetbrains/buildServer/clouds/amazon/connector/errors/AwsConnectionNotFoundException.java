package jetbrains.buildServer.clouds.amazon.connector.errors;

import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionNotFoundException extends AwsBuildFeatureException {
  public AwsConnectionNotFoundException(@NotNull final String connectionId) {
    super(String.format("Please, check that the AWS Connection with ID: <%s> exists and ensure that it is working", connectionId));
  }
  public AwsConnectionNotFoundException(@NotNull final Exception cause) {
    super(cause);
  }
}
