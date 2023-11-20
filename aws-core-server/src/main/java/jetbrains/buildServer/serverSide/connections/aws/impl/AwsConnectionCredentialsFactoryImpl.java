package jetbrains.buildServer.serverSide.connections.aws.impl;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.InvalidIdentifierException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.identifiers.IdentifiersUtil;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsConnectionCredentialsFactoryImpl implements AwsConnectionCredentialsFactory {

  private final ConcurrentMap<String, AwsCredentialsBuilder> myCredentialBuilders = new ConcurrentHashMap<>();

  public  AwsConnectionCredentialsFactoryImpl(@NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(ConnectionCredentialsFactory.class, AwsConnectionCredentialsFactoryImpl.class.getName(), this);
  }

  @NotNull
  @Deprecated
  @Override
  public ConnectionCredentials requestCredentials(@NotNull ConnectionDescriptor connectionDescriptor) throws ConnectionCredentialsException {
    String credentialsType = connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);

    AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
    AwsCredentialsHolder credentialsHolder = credentialsBuilder.constructSpecificCredentialsProvider(connectionDescriptor);

    return new AwsConnectionCredentials(credentialsHolder.getAwsCredentials(), connectionDescriptor.getParameters());
  }

  @NotNull
  @Override
  public ConnectionCredentials requestCredentials(@NotNull SProject project, @NotNull ConnectionDescriptor connectionDescriptor) throws ConnectionCredentialsException {
    if (!project.getProjectId().equals(connectionDescriptor.getProjectId()) && !ParamUtil.isAllowedInSubProjects(connectionDescriptor.getParameters())) {
      throw new ConnectionCredentialsException("Connection is not allowed to be used in subprojects");
    }

    return requestCredentials(connectionDescriptor);
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
  @Override
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> properties) {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      if (credentialsType == null) {
        throw new NoSuchAwsCredentialsBuilderException("Credentials type is null");
      }

      if (ParamUtil.isDefaultCredsProviderType(properties) && ParamUtil.isDefaultCredsProvidertypeDisabled()) {
        return Collections.singletonList(new InvalidProperty(CREDENTIALS_TYPE_PARAM, DISABLED_AWS_CONNECTION_TYPE_ERROR_MSG));
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
  public Map<String, String> getDefaultProperties() {
    Map<String, String> defaultProperties = new HashMap<>();
    myCredentialBuilders.forEach((type, builder) -> defaultProperties.putAll(builder.getDefaultProperties()));
    return defaultProperties;
  }

  @NotNull
  @Override
  public String describeAwsConnection(@NotNull final Map<String, String> properties) {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      AwsCredentialsBuilder credentialsBuilder = getAwsCredentialsBuilderOfType(credentialsType);
      return String.format(
        "Credentials Type: %s",
        credentialsBuilder.getPropertiesDescription(properties)
      );
    } catch (NoSuchAwsCredentialsBuilderException e) {
      return "Unsupported credentials type: " + credentialsType;
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

  private void validateConnectionId(@Nullable final String connectionId, @NotNull List<InvalidProperty> invalidProperties) {
    if (connectionId == null) {
      return;
    }

    try {
      IdentifiersUtil.validateExternalId(connectionId, "AWS Connection ID", IdentifiersUtil.EXT_ID_LENGTH);
    } catch (InvalidIdentifierException e) {
      invalidProperties.add(new InvalidProperty(USER_DEFINED_ID_PARAM, e.getMessage()));
    }
  }
}
