package jetbrains.buildServer.connections.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionDescriptorImpl;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsService;
import jetbrains.buildServer.serverSide.connections.credentials.errors.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.utils.ConnectionUtils;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionCredentialsFactoryImpl implements ConnectionCredentialsFactory<AwsConnectionDescriptor>, AwsCredentialsFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();
  private final ExtensionHolder myExtensionHolder;

  public AwsConnectionCredentialsFactoryImpl(@NotNull final ConnectionCredentialsService connectionCredentialsService,
                                             @NotNull final ExtensionHolder extensionHolder) {
    myExtensionHolder = extensionHolder;
    connectionCredentialsService.registerCredentialsFactory(this);
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor requestCredentials(@NotNull SProjectFeatureDescriptor connectionDescriptor) throws ConnectionCredentialsException {
    String credentialsType = connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      AwsCredentialsHolder credentialsHolder = credentialsBuilder.constructSpecificCredentialsProvider(connectionDescriptor);
      return new AwsConnectionDescriptorImpl(
        connectionDescriptor,
        credentialsHolder,
        ConnectionUtils.getConectionProviderOfType(myExtensionHolder, AwsConnectionProvider.TYPE),
        describeConnection(connectionDescriptor.getParameters())
      );

    } catch (NoSuchAwsCredentialsBuilderException e) {
      throw new ConnectionCredentialsException(e);
    }
  }

  @NotNull
  @Override
  public String getType() {
    return AwsConnectionProvider.TYPE;
  }

  @Override
  public void registerAwsCredentialsBuilder(@NotNull AwsCredentialsBuilder credentialsBuilder) {
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
  public String describeConnection(@NotNull Map<String, String> connectionProperties) {
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

  @NotNull
  @Override
  public PropertiesProcessor getPropertiesProcessor() {
    return new AwsConnectionPropertiesProcessor(this);
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    Map<String, String> defaultProperties = new HashMap<>();
    defaultProperties.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    defaultProperties.put(AwsAccessKeysParams.STS_ENDPOINT_PARAM, AwsCloudConnectorConstants.STS_ENDPOINT_DEFAULT);

    myCredentialBuilders.forEach((type, builder) -> {
      defaultProperties.putAll(builder.getDefaultProperties());
    });
    return defaultProperties;
  }

  @NotNull
  AwsCredentialsBuilder getAwsCredentialsBuilderOfType(@Nullable final String type) throws NoSuchAwsCredentialsBuilderException {
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

  public Class<AwsConnectionDescriptor> getConnectionDescriptorType() {
    return AwsConnectionDescriptor.class;
  }
}
