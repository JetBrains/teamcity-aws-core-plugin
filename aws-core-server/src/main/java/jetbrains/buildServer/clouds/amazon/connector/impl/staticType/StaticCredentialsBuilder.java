package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StaticCredentialsBuilder implements AwsCredentialsBuilder {

  private final ExecutorServices myExecutorServices;

  public StaticCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory,
                                  @NotNull final ExecutorServices executorServices) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
    myExecutorServices = executorServices;
  }

  @Override
  @NotNull
  public AwsCredentialsHolder constructConcreteCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException {

    List<InvalidProperty> invalidProperties = validateProperties(cloudConnectorProperties);
    processInvalidProperties(invalidProperties);

    if (ParamUtil.useSessionCredentials(cloudConnectorProperties)) {
      Loggers.CLOUD.debug("Using Session credentials for the AWS key: " + ParamUtil.maskKey(cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM)));
      return new StaticSessionCredentialsHolder(
        getBasicCredentialsProvider(cloudConnectorProperties),
        cloudConnectorProperties,
        myExecutorServices
      );
    } else {
      return getBasicCredentialsProvider(cloudConnectorProperties);
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) {
    List<InvalidProperty> invalidProperties = new ArrayList<>();

    if (StringUtil.isEmpty(properties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, "Please provide the access key ID"));
    }
    if (StringUtil.isEmpty(properties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, "Please provide the secret access key "));
    }

    if (StringUtil.isEmpty(StringUtil.emptyIfNull(properties.get(AwsCloudConnectorConstants.REGION_NAME_PARAM)))) {
      invalidProperties.add(new InvalidProperty(AwsCloudConnectorConstants.REGION_NAME_PARAM, "Please choose the region where this AWS Connection will be used"));
    }

    if (!ParamUtil.isValidSessionDuration(properties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsSessionCredentialsParams.SESSION_DURATION_PARAM, "Session duration is not valid"));
    }

    return invalidProperties;
  }

  private void processInvalidProperties(@NotNull final List<InvalidProperty> invalidProperties) throws AwsConnectorException {
    if (invalidProperties.size() > 0) {
      InvalidProperty lastInvalidProperty = invalidProperties.get(invalidProperties.size() - 1);
      String errorDescription = StringUtil.emptyIfNull(lastInvalidProperty.getInvalidReason());
      throw new AwsConnectorException(
        errorDescription,
        lastInvalidProperty.getPropertyName()
      );
    }
  }

  @NotNull
  private AwsCredentialsHolder getBasicCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) {
    return new StaticCredentialsHolder(cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM), cloudConnectorProperties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM));
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE;
  }

  @Override
  @NotNull
  public String getPropertiesDescription(@NotNull final Map<String, String> properties){
    return String.format(
      "Static key %s",
      properties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM)
    );
  }
}
