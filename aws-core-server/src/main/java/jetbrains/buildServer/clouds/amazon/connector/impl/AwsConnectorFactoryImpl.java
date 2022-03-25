package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public class AwsConnectorFactoryImpl implements AwsConnectorFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();

  @NotNull
  @Override
  public AWSCredentialsProvider buildAwsCredentialsProvider(@NotNull final Map<String, String> connectionProperties) {
    String credentialsType = connectionProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      return credentialsBuilder.constructConcreteCredentialsProvider(connectionProperties);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warn("Failed to create AWSCredentialsProvider: " + e.getMessage());
      return new BrokenCredentialsProvider();
    }
  }

  @NotNull
  @Override
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) throws NoSuchAwsCredentialsBuilderException {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
    return credentialsBuilder.validateProperties(properties);
  }

  @NotNull
  @Override
  public String describeAwsConnection(@NotNull final Map<String, String> connectionProperties) {
    return String.format(
      "Credentials Type: %s",
      connectionProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM)
    );
  }

  @Override
  public void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder) {
    String credentialsType = credentialsBuilder.getCredentialsType();
    AwsCredentialsBuilder existingBuilder = myCredentialBuilders.putIfAbsent(credentialsType, credentialsBuilder);
    if (existingBuilder != null && existingBuilder != credentialsBuilder) {
      throw new IllegalStateException(
        "Attempted to register AWS credentials credentialsBuilder for credentials type \"" + credentialsType +
        "\" when another one for this credentials type is already registered.");
    }
  }

  @NotNull
  private AwsCredentialsBuilder getAwsCredentialsBuilderOfType(final String type) throws NoSuchAwsCredentialsBuilderException {
    AwsCredentialsBuilder builder = myCredentialBuilders.get(type);
    if (builder == null) {
      String errMsg = "Failed to find registered AwsCredentialsBuilder for type " + type + ".";
      throw new NoSuchAwsCredentialsBuilderException(errMsg);
    }
    return builder;
  }
}
