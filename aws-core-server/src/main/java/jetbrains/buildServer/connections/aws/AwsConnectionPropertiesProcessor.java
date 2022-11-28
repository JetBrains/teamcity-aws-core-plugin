package jetbrains.buildServer.connections.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidIdentifierException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.identifiers.IdentifiersUtil;
import jetbrains.buildServer.serverSide.impl.IdGeneratorRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionPropertiesProcessor implements PropertiesProcessor {

  private final AwsConnectionCredentialsFactoryImpl myAwsConnectionCredentialsFactory;

  public AwsConnectionPropertiesProcessor(@NotNull final AwsConnectionCredentialsFactoryImpl awsConnectionCredentialsFactory) {
    myAwsConnectionCredentialsFactory = awsConnectionCredentialsFactory;
  }

  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    String credentialsType = properties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
    try {
      if (credentialsType == null) {
        throw new NoSuchAwsCredentialsBuilderException("Credentials type is null");
      }

      AwsCredentialsBuilder credentialsBuilder = myAwsConnectionCredentialsFactory.getAwsCredentialsBuilderOfType(credentialsType);
      List<InvalidProperty> invalidProperties = credentialsBuilder.validateProperties(properties);

      validateConnectionId(properties.get(USER_DEFINED_ID_PARAM), invalidProperties);

      return invalidProperties;

    } catch (NoSuchAwsCredentialsBuilderException e) {
      List<InvalidProperty> invalidProperties = new ArrayList<>();
      invalidProperties.add(new InvalidProperty(CREDENTIALS_TYPE_PARAM, "The credentials type " + credentialsType + " is not supported."));
      return invalidProperties;
    }
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
