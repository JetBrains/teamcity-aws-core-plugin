package jetbrains.buildServer.serverSide.connections.aws.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.aws.AwsCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionCredentialsFactoryImpl implements ConnectionCredentialsFactory, AwsCredentialsFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();

  public AwsConnectionCredentialsFactoryImpl(@NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(ConnectionCredentialsFactory.class, AwsConnectionCredentialsFactoryImpl.class.getName(), this);
  }

  @NotNull
  @Override
  public ConnectionCredentials requestCredentials(@NotNull ConnectionDescriptor connectionDescriptor) throws ConnectionCredentialsException {
    String credentialsType = connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);

    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
    AwsCredentialsHolder credentialsHolder = credentialsBuilder.constructSpecificCredentialsProvider(connectionDescriptor);

    return new AwsConnectionCredentials(credentialsHolder.getAwsCredentials(), connectionDescriptor.getParameters());
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
        "Attempted to register AWS credentialsBuilder for credentials type \"" + credentialsType +
        "\" when another one for this credentials type is already registered.");
    }
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
}
