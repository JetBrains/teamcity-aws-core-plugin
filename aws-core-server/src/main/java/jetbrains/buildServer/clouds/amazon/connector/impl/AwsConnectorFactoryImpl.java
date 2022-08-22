package jetbrains.buildServer.clouds.amazon.connector.impl;

import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectorFactoryImpl implements AwsConnectorFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();
  private final AwsConnectionIdGenerator myAwsConnectionIdGenerator;
  public AwsConnectorFactoryImpl(@NotNull final AwsConnectionIdGenerator awsConnectionIdGenerator){
    myAwsConnectionIdGenerator = awsConnectionIdGenerator;
  }

  @NotNull
  @Override
  public AwsCredentialsHolder buildAwsCredentialsProvider(@NotNull final Map<String, String> connectionProperties) throws AwsConnectorException {
    String credentialsType = connectionProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);

    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
    return credentialsBuilder.constructConcreteCredentialsProvider(connectionProperties);
  }

  @NotNull
  @Override
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties) {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      if(credentialsType == null){
        throw new NoSuchAwsCredentialsBuilderException("Credentials type is null");
      }

      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      List<InvalidProperty> invalidProperties = credentialsBuilder.validateProperties(properties);

      String userDefinedId = properties.get(USER_DEFINED_ID_PARAM);
      if (userDefinedId != null && ! myAwsConnectionIdGenerator.isUnique(userDefinedId)) {
        invalidProperties.add(new InvalidProperty(USER_DEFINED_ID_PARAM, "The Connection ID must be unique on the whole server"));
      }

      return invalidProperties;

    } catch (NoSuchAwsCredentialsBuilderException e){
      List<InvalidProperty> invalidProperties = new ArrayList<>();
      invalidProperties.add(new InvalidProperty(CREDENTIALS_TYPE_PARAM, "The credentials type " + credentialsType + " is not supported."));
      return invalidProperties;
    }
  }

  @NotNull
  @Override
  public String describeAwsConnection(@NotNull final Map<String, String> connectionProperties) {
    String credentialsType = connectionProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      return String.format(
        "Credentials Type: %s",
        credentialsBuilder.getPropertiesDescription(connectionProperties)
      );
    } catch (NoSuchAwsCredentialsBuilderException e) {
      return "Unsupported credentials type: " + credentialsType;
    }
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
  private AwsCredentialsBuilder getAwsCredentialsBuilderOfType(@Nullable final String type) throws NoSuchAwsCredentialsBuilderException {
    if(type == null){
      String errMsg = "There is no credentials type property in the AWS Connection, cannot construct Credentials Provider of type null.";
      throw new NoSuchAwsCredentialsBuilderException(errMsg);
    }

    AwsCredentialsBuilder builder = myCredentialBuilders.get(type);
    if (builder == null) {
      String errMsg = "Failed to find registered AwsCredentialsBuilder for type " + type + ".";
      throw new NoSuchAwsCredentialsBuilderException(errMsg);
    }
    return builder;
  }
}
