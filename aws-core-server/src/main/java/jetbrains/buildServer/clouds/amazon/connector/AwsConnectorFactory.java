package jetbrains.buildServer.clouds.amazon.connector;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectorFactory {

  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);

  @NotNull
  AwsCredentialsBuilder getAwsCredentialsBuilderOfType(@NotNull final String type) throws NoSuchAwsCredentialsBuilderException;

  @NotNull
  List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) throws NoSuchAwsCredentialsBuilderException;

  @NotNull
  String describeAwsConnection(@NotNull final Map<String, String> connectionProperties);
}
