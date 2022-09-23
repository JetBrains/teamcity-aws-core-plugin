package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CHOSEN_AWS_CONN_NAME_PARAM;

public class AwsConnToAgentBuildFeature extends BuildFeature implements PropertiesProcessor {

  private static final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToEnvVars/editAwsConnToEnvVarsBuildFeature.jsp";
  private static final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME;

  private final String myPluginResourcesEditUrl;
  public static final String DISPLAY_NAME = "Add AWS credentials to the build";

  public AwsConnToAgentBuildFeature(@NotNull final PluginDescriptor pluginDescriptor) {
    myPluginResourcesEditUrl = pluginDescriptor.getPluginResourcesPath(EDIT_PARAMETERS_URL);
  }

  @NotNull
  public static Collection<SBuildFeatureDescriptor> getAwsConnectionsToExpose(@NotNull final SBuild build) {
    return build.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE);
  }

  @Override
  @NotNull
  public Collection<InvalidProperty> process(@NotNull final Map<String, String> properties) {
    ChosenAwsConnPropertiesProcessor awsConnsPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
    return awsConnsPropertiesProcessor.process(properties);
  }

  @Override
  public PropertiesProcessor getParametersProcessor() {
    return this;
  }

  @NotNull
  @Override
  public String getType() {
    return AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myPluginResourcesEditUrl;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> params) {
    StringBuilder connDisplayNameBuilder = new StringBuilder();
    String connDisplayName = params.get(CHOSEN_AWS_CONN_NAME_PARAM);
    if (connDisplayName != null) {
      connDisplayNameBuilder.append("\"");
      connDisplayNameBuilder.append(connDisplayName);
      connDisplayNameBuilder.append("\"");
    }
    return "Adds credentials of AWS Connection " + connDisplayNameBuilder + " to the build";
  }

  public String getAvailAwsConnsUrl() {
    return AVAIL_AWS_CONNS_URL;
  }
}
