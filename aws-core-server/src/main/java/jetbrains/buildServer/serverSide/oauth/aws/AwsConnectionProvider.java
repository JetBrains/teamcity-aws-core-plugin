package jetbrains.buildServer.serverSide.oauth.aws;

import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsConnectionProvider extends OAuthProvider {

  public static final String TYPE = AwsCloudConnectorConstants.CLOUD_TYPE;
  public static final String EDIT_PARAMS_URL = "awsConnection/editAwsConnection.jsp";

  private final String myEditParametersUrl;

  private final AwsConnectionCredentialsFactory myAwsConnectionCredentialsFactory;


  public AwsConnectionProvider(@NotNull final PluginDescriptor descriptor, @NotNull final AwsConnectionCredentialsFactory awsConnectionCredentialsFactory) {
    myEditParametersUrl = descriptor.getPluginResourcesPath(EDIT_PARAMS_URL);
    myAwsConnectionCredentialsFactory = awsConnectionCredentialsFactory;
  }

  @Override
  @NotNull
  public String getType() {
    return TYPE;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Amazon Web Services (AWS)";
  }


  @Override
  @NotNull
  public String describeConnection(@NotNull final OAuthConnectionDescriptor connection) {
    return myAwsConnectionCredentialsFactory.describeAwsConnection(connection.getParameters());
  }

  @Override
  @NotNull
  public PropertiesProcessor getPropertiesProcessor() {
    return map -> {
      final List<InvalidProperty> invalidProperties = new ArrayList<>();

      String credentialsType = map.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM);
      if (StringUtil.isEmpty(credentialsType)) {
        invalidProperties.add(new InvalidProperty(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "Please choose a credentials type."));
      } else {
        invalidProperties.addAll(myAwsConnectionCredentialsFactory.getInvalidProperties(map));
      }

      return invalidProperties;
    };
  }

  @Override
  @Nullable
  public Map<String, String> getDefaultProperties() {
    Map<String, String> defaultProperties = new HashMap<>();
    defaultProperties.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    defaultProperties.put(AwsAccessKeysParams.STS_ENDPOINT_PARAM, AwsCloudConnectorConstants.STS_ENDPOINT_DEFAULT);
    defaultProperties.put(AwsSessionCredentialsParams.SESSION_DURATION_PARAM, AwsSessionCredentialsParams.SESSION_DURATION_DEFAULT);

    defaultProperties.putAll(myAwsConnectionCredentialsFactory.getDefaultProperties());
    return defaultProperties;
  }

  @Override
  @NotNull
  public String getEditParametersUrl() {
    return myEditParametersUrl;
  }
}
