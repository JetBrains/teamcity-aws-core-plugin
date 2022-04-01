package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import java.util.*;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
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

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @NotNull
  public static List<OAuthConnectionDescriptor> getLinkedAwsConnections(@NotNull final SBuild build,
                                                                        @NotNull final OAuthConnectionsManager oAuthConnectionsManager) {
    try {
      if (build.getBuildId() < 0) {
        throw new AwsBuildFeatureException("Dummy build with negative id does not have AWS Connections to expose.");
      }

      SBuildType buildType = build.getBuildType();
      if (buildType == null) {
        throw new AwsBuildFeatureException("There is no BuildType for the Build with id: " + build.getBuildId());
      }

      BuildSettings buildSettings = ((BuildPromotionEx)build.getBuildPromotion()).getBuildSettings();

      SBuildFeatureDescriptor configuredAwsConnBuildFeature;
      try {
        configuredAwsConnBuildFeature = buildSettings.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE).iterator().next();
      } catch (NoSuchElementException nsee) {
        return new ArrayList<>();
      }

      String awsConnectionId = configuredAwsConnBuildFeature.getParameters().get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
      if (awsConnectionId == null) {
        throw new AwsBuildFeatureException("AWS Connetion ID to expose is null.");
      }

      OAuthConnectionDescriptor connectionDescriptor = oAuthConnectionsManager.findConnectionById(buildType.getProject(), awsConnectionId);
      if (connectionDescriptor == null) {
        throw new AwsBuildFeatureException("Could not find AWS Connection to expose, Connection ID: " + awsConnectionId);
      }

      return Arrays.asList(connectionDescriptor);

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Got an exception while getting AWS Connection to expose: " + e.getMessage());
      return new ArrayList<>();
    }
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
