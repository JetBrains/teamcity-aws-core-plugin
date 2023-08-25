package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface AwsConnectionCredentialsFactory extends ConnectionCredentialsFactory {
  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);
  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties);
  @NotNull
  Map<String, String> getDefaultProperties();
  @NotNull
  String describeAwsConnection(@NotNull final Map<String, String> properties);
}
