package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class StaticCredentialsBuilder implements AwsCredentialsBuilder {

  private final ExecutorServices myExecutorServices;

  public StaticCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory,
                                  @NotNull final ExecutorServices executorServices) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
    myExecutorServices = executorServices;
  }

  @Override
  @NotNull
  public AWSCredentialsProvider constructConcreteCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException {

    List<InvalidProperty> invalidProperties = validateProperties(cloudConnectorProperties);
    processInvalidProperties(invalidProperties);

    if (ParamUtil.useSessionCredentials(cloudConnectorProperties)) {
      return new StaticSessionCredentialsProvider(
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

    if (!ParamUtil.isValidSessionDuration(properties.get(AwsAccessKeysParams.SESSION_DURATION_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsAccessKeysParams.SESSION_DURATION_PARAM, "Session duration is not valid"));
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
  private AWSCredentialsProvider getBasicCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) {
    return new AWSCredentialsProvider() {
      @Override
      public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(
          cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
          cloudConnectorProperties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM)
        );
      }

      @Override
      public void refresh() {
        //
      }
    };
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE;
  }
}
