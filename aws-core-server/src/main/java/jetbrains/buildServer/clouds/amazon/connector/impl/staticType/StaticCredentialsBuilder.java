package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.impl.BaseAwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.impl.CredentialsRefresher;
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

public class StaticCredentialsBuilder extends BaseAwsCredentialsBuilder {

  private final ExecutorServices myExecutorServices;

  public StaticCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory,
                                  @NotNull final ExecutorServices executorServices) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
    myExecutorServices = executorServices;
  }

  @NotNull
  @Override
  protected AwsCredentialsHolder constructConcreteCredentialsProviderImpl(@NotNull final Map<String, String> cloudConnectorProperties) {
    if (ParamUtil.useSessionCredentials(cloudConnectorProperties)) {
      Loggers.CLOUD.debug("Using Session credentials for the AWS key: " + ParamUtil.maskKey(cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM)));
      return createSessionCredentialsHolder(cloudConnectorProperties);
    } else {
      return getBasicCredentialsProvider(cloudConnectorProperties);
    }
  }

  @NotNull
  @Override
  public AwsCredentialsHolder requestNewSessionWithDuration(@NotNull Map<String, String> parameters) {
    //TODO: TW-77164 use one-time request after we stop scheduling the refresh task
    return constructConcreteCredentialsProviderImpl(parameters);
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
    return "Static IAM Access Key";
  }

  @NotNull
  protected CredentialsRefresher createSessionCredentialsHolder(@NotNull final Map<String, String> cloudConnectorProperties){
    return new StaticSessionCredentialsHolder(
      getBasicCredentialsProvider(cloudConnectorProperties),
      cloudConnectorProperties,
      myExecutorServices
    );
  }
}
