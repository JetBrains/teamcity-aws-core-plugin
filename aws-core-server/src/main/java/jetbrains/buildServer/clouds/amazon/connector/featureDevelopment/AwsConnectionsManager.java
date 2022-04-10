package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.Map;
import java.util.NoSuchElementException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.NoLinkedAwsConnectionException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionsManager {
  private final OAuthConnectionsManager myConnectionsManager;

  public AwsConnectionsManager(@NotNull final OAuthConnectionsManager connectionsManager) {
    myConnectionsManager = connectionsManager;
  }

  @NotNull
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> properties, @NotNull final SProject project) throws NoLinkedAwsConnectionException {
    String awsConnectionId = properties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new NoLinkedAwsConnectionException("AWS Connetion ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property.");
    }

    OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(project, awsConnectionId);
    if (connectionDescriptor == null) {
      throw new NoLinkedAwsConnectionException("Could not find linked AWS Connection, Connection ID: " + awsConnectionId);
    }

    return new AwsConnectionBean(connectionDescriptor);
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @NotNull
  public AwsConnectionBean getAwsConnectionForBuild(@NotNull final SBuild build) {
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
        return new AwsConnectionBean();
      }

      return getLinkedAwsConnection(configuredAwsConnBuildFeature.getParameters(), buildType.getProject());

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Got an exception while getting AWS Connection to expose: " + e.getMessage());
      return new AwsConnectionBean();
    }
  }
}