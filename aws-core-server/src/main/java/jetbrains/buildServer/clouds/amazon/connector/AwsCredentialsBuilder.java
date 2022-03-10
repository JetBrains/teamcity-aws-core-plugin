package jetbrains.buildServer.clouds.amazon.connector;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public interface AwsCredentialsBuilder {

  @NotNull
  AWSCredentialsProvider createCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException;

  @NotNull
  List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties);

  @NotNull
  String getCredentialsType();
}
