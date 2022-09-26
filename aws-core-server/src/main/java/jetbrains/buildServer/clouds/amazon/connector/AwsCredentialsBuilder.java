package jetbrains.buildServer.clouds.amazon.connector;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface AwsCredentialsBuilder {

  /**
   * Implemented in each AwsCredentialsBuilder in a different way to support
   * various types of obtaining credentials.
   * The {@link jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants#CREDENTIALS_TYPE_PARAM Credentials Type property}
   * should exist in cloudConnectorProperties argument to distinguish which AwsCredentialsBuilder should be used to build the AWSCredentialsProvider object.
   * <p>
   * <b>Do not use this method directly</b>,
   * instead, call the {@link jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory#buildAwsCredentialsProvider(Map) AwsConnectorFactory#buildAwsCredentialsProvider(connectionProperties)}
   * method, to ensure that the correct AwsCredentialsBuilder will be used to provide AWS credentials.
   *
   * @param  featureDescriptor  Connection descriptor with properties Map of concrete AWS Connection.
   * @return AwsCredentialsHolder object with specified credentials type.
   * @see    jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory#buildAwsCredentialsProvider(Map) buildAwsCredentialsProvider(connectionProperties).
   */
  @NotNull
  AwsCredentialsHolder constructSpecificCredentialsProvider(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;

  @NotNull
  List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties);

  @NotNull
  String getCredentialsType();

  @NotNull
  String getPropertiesDescription(@NotNull final Map<String, String> properties);
}
