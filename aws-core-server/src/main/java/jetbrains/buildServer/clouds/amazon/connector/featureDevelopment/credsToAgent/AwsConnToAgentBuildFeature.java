package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_DEFAULT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

public class AwsConnToAgentBuildFeature extends BuildFeature implements PropertiesProcessor {

  private static final String EDIT_PARAMETERS_URL = "awsConnection/buildFeatures/awsConnToAgent/editAwsConnToAgentBuildFeature.jsp";
  private static final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME;

  private final String myPluginResourcesEditUrl;
  public static final String DISPLAY_NAME = "AWS Credentials";
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

  @Override
  public Map<String, String> getDefaultParameters() {
    return Collections.singletonMap(SESSION_DURATION_PARAM, SESSION_DURATION_DEFAULT);
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
    StringBuilder featureDescriptionBuilder = new StringBuilder();
    featureDescriptionBuilder.append("Adds credentials of AWS Connection ");

    String connId = params.get(CHOSEN_AWS_CONN_ID_PARAM);
    if (connId != null) {
      try {
        AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionsManager.getAwsConnection(connId);
        featureDescriptionBuilder.append("\"");
        featureDescriptionBuilder.append(awsConnectionDescriptor.getParameters().get(OAuthConstants.DISPLAY_NAME_PARAM));
        featureDescriptionBuilder.append("\"");
      } catch (AwsConnectorException e) {
        String erroMessage = "Cannot get description for AWS Connection with ID " + connId;
        Loggers.CLOUD.warnAndDebugDetails(erroMessage + " in BuildFeature", e);
        return erroMessage;
      }
    }
    featureDescriptionBuilder.append(" to the build");
    String sessionDurationParam = params.get(SESSION_DURATION_PARAM);
    if (sessionDurationParam != null && ParamUtil.isValidSessionDuration(sessionDurationParam)) {
      featureDescriptionBuilder.append(String.format(", credentials will be valid for %s minutes", sessionDurationParam));
    }

    return featureDescriptionBuilder.toString();
  }

  public String getAvailAwsConnsUrl() {
    return AVAIL_AWS_CONNS_URL;
  }
}
