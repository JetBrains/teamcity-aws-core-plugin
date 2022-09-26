package jetbrains.buildServer.clouds.amazon.connector.impl.defaultProviderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.BaseAwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME;

public class DefaultProviderCredentialsBuilder extends BaseAwsCredentialsBuilder {

  public DefaultProviderCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
  }

  @NotNull
  @Override
  protected AwsCredentialsHolder constructSpecificCredentialsProviderImpl(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    if (! TeamCityProperties.getBooleanOrTrue(DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME)) {
      throw new AwsConnectorException("Default Credentials Provider is disabled on this server, please, contact the server Administrator");
    }
    return new DefaultProviderCredentialsHolder();
  }

  @NotNull
  @Override
  public AwsCredentialsHolder requestNewSessionWithDuration(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    return constructSpecificCredentialsProviderImpl(featureDescriptor);
  }

  @Override
  @NotNull
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) {
    return new ArrayList<>();
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.DEFAULT_PROVIDER_CREDENTIALS_TYPE;
  }

  @Override
  @NotNull
  public String getPropertiesDescription(@NotNull final Map<String, String> properties) {
    return "Default way of obtaining credentials";
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    return Collections.emptyMap();
  }
}
