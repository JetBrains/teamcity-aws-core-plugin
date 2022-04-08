package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnToEnvVarsBuildFeature extends BuildFeature implements PropertiesProcessor {

  private final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToEnvVars/editAwsConnToEnvVrasBuildFeature.jsp";
  private final String myPluginResourcesEditUrl;
  private final String displayName = "Expose AWS Credentials via Env Vars";

  public AwsConnToEnvVarsBuildFeature(@NotNull final PluginDescriptor pluginDescriptor) {
    myPluginResourcesEditUrl = pluginDescriptor.getPluginResourcesPath(EDIT_PARAMETERS_URL);
  }

  @Override
  @NotNull
  public Collection<InvalidProperty> process(@NotNull final Map<String, String> properties) {
    ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();
    if (StringUtil.nullIfEmpty(properties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM)) == null) {
      invalidProperties.add(new InvalidProperty(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, "AWS Connection was not specified"));
    }
    return invalidProperties;
  }

  @NotNull
  @Override
  public String getType() {
    return AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myPluginResourcesEditUrl;
  }
}
