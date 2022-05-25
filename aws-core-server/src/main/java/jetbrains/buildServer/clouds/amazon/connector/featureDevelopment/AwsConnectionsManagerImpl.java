package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.Map;
import java.util.NoSuchElementException;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionsManagerImpl implements AwsConnectionsManager {
  private final OAuthConnectionsManager myConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionsManagerImpl(@NotNull final OAuthConnectionsManager connectionsManager,
                                   @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myConnectionsManager = connectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Nullable
  @Override
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> properties, @NotNull final SProject project) throws LinkedAwsConnNotFoundException {
    String awsConnectionId = properties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new LinkedAwsConnNotFoundException("AWS Connection ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property.");
    }

    OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(project, awsConnectionId);
    if (connectionDescriptor == null) {
      return null;
    }

    try {
      return AwsConnectionUtils.awsConnBeanFromDescriptor(connectionDescriptor, myAwsConnectorFactory);
    } catch (AwsConnectorException awsConnectorException) {
      throw new LinkedAwsConnNotFoundException("Could not get the AWS Credentials: " + awsConnectorException.getMessage());
    }
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @Nullable
  @Override
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
        return null;
      }

      return getLinkedAwsConnection(configuredAwsConnBuildFeature.getParameters(), buildType.getProject());

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Got an exception while getting AWS Connection to expose: " + e.getMessage());
      return null;
    }
  }
}