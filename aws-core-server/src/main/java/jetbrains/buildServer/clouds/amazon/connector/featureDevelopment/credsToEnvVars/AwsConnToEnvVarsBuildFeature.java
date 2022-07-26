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

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsConnToEnvVarsBuildFeature extends BuildFeature implements PropertiesProcessor {

  private final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToEnvVars/editAwsConnToEnvVarsBuildFeature.jsp";
  private final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME;

  private final String myPluginResourcesEditUrl;
  private final String displayName = "Put AWS credentials to agent environment variables";

  public AwsConnToEnvVarsBuildFeature(@NotNull final PluginDescriptor pluginDescriptor) {
    myPluginResourcesEditUrl = pluginDescriptor.getPluginResourcesPath(EDIT_PARAMETERS_URL);
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
    StringBuilder connDisplayNameBuilder = new StringBuilder();
    String connDisplayName = params.get(CHOSEN_AWS_CONN_NAME_PARAM);
    if (connDisplayName != null) {
      connDisplayNameBuilder.append("\"");
      connDisplayNameBuilder.append(connDisplayName);
      connDisplayNameBuilder.append("\"");
    }
    return "Adds credentials of AWS Connection " + connDisplayNameBuilder + " to environment variables";
  }

  public String getAvailAwsConnsUrl() {
    return AVAIL_AWS_CONNS_URL;
  }
}
