package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsConnToAgentBuildFeature extends BuildFeature implements PropertiesProcessor {

  private static final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToEnvVars/editAwsConnToEnvVarsBuildFeature.jsp";
  private static final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME;

  private final String myPluginResourcesEditUrl;
  public static final String DISPLAY_NAME = "Add AWS credentials to the build";
  private final AwsConnectionsManager myAwsConnectionsManager;

  public AwsConnToAgentBuildFeature(@NotNull final PluginDescriptor pluginDescriptor,
                                      @NotNull final AwsConnectionsManager awsConnectionsManager) {
    myAwsConnectionsManager = awsConnectionsManager;
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
    String connId = params.get(CHOSEN_AWS_CONN_ID_PARAM);
    if (connId != null) {
      try {
        AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionsManager.getAwsConnection(connId);
        connDisplayNameBuilder.append("\"");
        connDisplayNameBuilder.append(awsConnectionDescriptor.getParameters().get(OAuthConstants.DISPLAY_NAME_PARAM));
        connDisplayNameBuilder.append("\"");
      } catch (AwsConnectorException e) {
        throw new RuntimeException(e);
      }
    }
    return "Adds credentials of AWS Connection " + connDisplayNameBuilder + " to the build";
  }

  public String getAvailAwsConnsUrl() {
    return AVAIL_AWS_CONNS_URL;
  }
}
