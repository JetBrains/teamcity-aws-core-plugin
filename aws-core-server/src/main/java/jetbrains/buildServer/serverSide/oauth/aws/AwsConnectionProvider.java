package jetbrains.buildServer.serverSide.oauth.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionProvider extends OAuthProvider {

  public static final String TYPE = AwsCloudConnectorConstants.CLOUD_TYPE;
  public static final String EDIT_PARAMS_URL = "awsConnection/editAwsConnection.jsp";

  private final String myEditParametersUrl;

  private final AwsConnectorFactory myAwsConnectorFactory;


  public AwsConnectionProvider(@NotNull final PluginDescriptor descriptor, @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myEditParametersUrl = descriptor.getPluginResourcesPath(EDIT_PARAMS_URL);
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public String getType() {
    return TYPE;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Amazon Web Services";
  }


  @Override
  @NotNull
  public String describeConnection(@NotNull final OAuthConnectionDescriptor connection) {
    return myAwsConnectorFactory.describeAwsConnection(connection.getParameters());
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
        invalidProperties.addAll(myAwsConnectorFactory.getInvalidProperties(map));
      }

      return invalidProperties;
    };
  }

  @Override
  @Nullable
  public Map<String, String> getDefaultProperties() {
    return Collections.emptyMap();
  }

  @Override
  @NotNull
  public String getEditParametersUrl() {
    return myEditParametersUrl;
  }
}
