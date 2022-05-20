package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNS_JSP_FILE_NAME;

public class AwsConnToEnvVarsBuildFeature extends BuildFeature implements PropertiesProcessor {

  private final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToEnvVars/editAwsConnToEnvVrasBuildFeature.jsp";
  private final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_JSP_FILE_NAME;

  private final String myPluginResourcesEditUrl;
  private final String displayName = "Expose AWS Credentials via Env Vars";

  public AwsConnToEnvVarsBuildFeature(@NotNull final PluginDescriptor pluginDescriptor) {
    myPluginResourcesEditUrl = pluginDescriptor.getPluginResourcesPath(EDIT_PARAMETERS_URL);
  }

  @Override
  @NotNull
  public Collection<InvalidProperty> process(@NotNull final Map<String, String> properties) {
    ChosenAwsConnPropertiesProcessor awsConnsPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
    return awsConnsPropertiesProcessor.process(properties);
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

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> params) {
    return "AWS Connection ID - the id of Connection which AWS Credentials will be exposed to environment variables.";
  }

  public String getAvailAwsConnsUrl() {
    return AVAIL_AWS_CONNS_URL;
  }
}
