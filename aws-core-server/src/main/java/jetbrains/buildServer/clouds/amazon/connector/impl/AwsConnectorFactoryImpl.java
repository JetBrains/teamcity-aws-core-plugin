package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.InvalidIdentifierException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.identifiers.IdentifiersUtil;
import jetbrains.buildServer.serverSide.impl.IdGeneratorRegistry;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.DISABLED_AWS_CONNECTION_TYPE_ERROR_MSG;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectorFactoryImpl implements AwsConnectorFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();

  @NotNull
  @Override
  public AwsCredentialsHolder buildAwsCredentialsProvider(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    String credentialsType = featureDescriptor.getParameters().get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);

    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
    return credentialsBuilder.constructSpecificCredentialsProvider(featureDescriptor);
  }

  @NotNull
  @Override
  public AwsCredentialsHolder requestNewSessionWithDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull final String sessionDuration)
    throws AwsConnectorException {
    String credentialsType = featureDescriptor.getParameters().get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);

    Map<String, String> paramsWithSessionDuration = new HashMap<>(featureDescriptor.getParameters());
    paramsWithSessionDuration.put(AwsSessionCredentialsParams.SESSION_DURATION_PARAM, sessionDuration);
    return credentialsBuilder.constructSpecificCredentialsProvider(
      new ProjectFeatureDescriptorImpl(
        featureDescriptor.getId(),
        featureDescriptor.getType(),
        paramsWithSessionDuration,
        featureDescriptor.getProjectId()
      )
    );
  }

  @NotNull
  @Override
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties) {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      if (credentialsType == null) {
        throw new NoSuchAwsCredentialsBuilderException("Credentials type is null");
      }

      if (ParamUtil.isDefaultCredsProviderType(properties) && ParamUtil.isDefaultCredsProvidertypeDisabled()) {
        return Arrays.asList(new InvalidProperty(CREDENTIALS_TYPE_PARAM, DISABLED_AWS_CONNECTION_TYPE_ERROR_MSG));
      }

      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      List<InvalidProperty> invalidProperties = credentialsBuilder.validateProperties(properties);

      validateConnectionId(properties.get(USER_DEFINED_ID_PARAM), invalidProperties);

      return invalidProperties;

    } catch (NoSuchAwsCredentialsBuilderException e) {
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
  @Override
  public Map<String, String> getDefaultProperties() {
    Map<String, String> defaultProperties = new HashMap<>();
    defaultProperties.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    defaultProperties.put(AwsAccessKeysParams.STS_ENDPOINT_PARAM, AwsCloudConnectorConstants.STS_ENDPOINT_DEFAULT);
    putIgnoredDefaultSessionDurationValue(defaultProperties);

    myCredentialBuilders.forEach((type, builder) -> {
      defaultProperties.putAll(builder.getDefaultProperties());
    });
    return defaultProperties;
  }

  private void putIgnoredDefaultSessionDurationValue(@NotNull Map<String, String> defaultProperties) {
    defaultProperties.put(AwsSessionCredentialsParams.SESSION_DURATION_PARAM, AwsSessionCredentialsParams.SESSION_DURATION_DEFAULT);
  }

  @NotNull
  private AwsCredentialsBuilder getAwsCredentialsBuilderOfType(@Nullable final String type) throws NoSuchAwsCredentialsBuilderException {
    if (type == null) {
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

  private void validateConnectionId(@Nullable final String connectionId, @NotNull List<InvalidProperty> invalidProperties) {
    if (connectionId == null) {
      return;
    }

    try {
      StringBuilder errorMessageBuilder = new StringBuilder();
      errorMessageBuilder.append("This ID is invalid, please, dont use these symbols: ");
      errorMessageBuilder.append(IdGeneratorRegistry.PROHIBITED_CHARS);
      IdentifiersUtil.validateExternalId(connectionId, "AWS Connection ID", IdentifiersUtil.EXT_ID_LENGTH);
    } catch (InvalidIdentifierException e) {
      invalidProperties.add(new InvalidProperty(USER_DEFINED_ID_PARAM, e.getMessage()));
    }
  }
}
