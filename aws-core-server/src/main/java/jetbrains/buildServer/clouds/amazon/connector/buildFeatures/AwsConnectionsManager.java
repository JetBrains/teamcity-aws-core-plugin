package jetbrains.buildServer.clouds.amazon.connector.buildFeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsBuildFeatureException;
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
  public List<AwsConnectionBean> getAwsConnectionsForBuild(@NotNull final SBuild build){
    List<AwsConnectionBean> connectionBeans = new ArrayList<>();

    List<OAuthConnectionDescriptor> connectionDescriptors = getAwsConnsDescriptorsForBuild(build);
    for (OAuthConnectionDescriptor connectionDescriptor : connectionDescriptors) {
      AwsConnectionBean connectionBean = new AwsConnectionBean(connectionDescriptor);
      connectionBeans.add(connectionBean);
    }

    return connectionBeans;
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @NotNull
  private List<OAuthConnectionDescriptor> getAwsConnsDescriptorsForBuild(@NotNull final SBuild build) {
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

      OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(buildType.getProject(), awsConnectionId);
      if (connectionDescriptor == null) {
        throw new AwsBuildFeatureException("Could not find AWS Connection to expose, Connection ID: " + awsConnectionId);
      }

      return Arrays.asList(connectionDescriptor);

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Got an exception while getting AWS Connection to expose: " + e.getMessage());
      return new ArrayList<>();
    }
  }
}