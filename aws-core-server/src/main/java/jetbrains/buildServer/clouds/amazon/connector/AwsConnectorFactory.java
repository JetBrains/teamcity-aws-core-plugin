package jetbrains.buildServer.clouds.amazon.connector;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface AwsConnectorFactory {

  /**
   * Creates <b>AWSCredentialsProvider</b> using connection properties.
   * The credentials type will be extracted from the parameters.
   * @param  connectionProperties  Connection feature properties.
   * @return AWSCredentialsProvider object with needed credentials type.
   */
  @NotNull
  AwsCredentialsHolder buildAwsCredentialsProvider(@NotNull final Map<String, String> connectionProperties) throws AwsConnectorException;

  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);

  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties);

  @NotNull
  String describeAwsConnection(@NotNull final Map<String, String> connectionProperties);
}
