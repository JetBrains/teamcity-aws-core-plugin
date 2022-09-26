package jetbrains.buildServer.clouds.amazon.connector;

import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface AwsConnectorFactory {

  /**
   * Creates <b>AWSCredentialsProvider</b> using connection properties.
   * The credentials type will be extracted from the parameters.
   * @param  featureDescriptor  Connection feature descriptor with properties.
   * @return AWSCredentialsProvider object with needed credentials type.
   */
  @NotNull
  AwsCredentialsHolder buildAwsCredentialsProvider(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;

  @NotNull
  AwsCredentialsHolder requestNewSessionWithDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull final String sessionDuration) throws AwsConnectorException;

  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);

  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties);

  @NotNull
  String describeAwsConnection(@NotNull final Map<String, String> connectionProperties);

  @NotNull
  Map<String, String> getDefaultProperties();
}
