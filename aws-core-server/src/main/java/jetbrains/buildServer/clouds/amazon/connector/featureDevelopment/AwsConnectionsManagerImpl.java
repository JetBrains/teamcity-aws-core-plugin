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

  @NotNull
  @Override
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> featureProperties, @NotNull final SProject project) throws LinkedAwsConnNotFoundException {
    String awsConnectionId = featureProperties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new LinkedAwsConnNotFoundException("AWS Connection ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property. Project ID: " + project.getExternalId());
    }

    OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(project, awsConnectionId);
    if (connectionDescriptor == null) {
      throw new LinkedAwsConnNotFoundException("Could not find the AWS Connection with ID " + awsConnectionId + " in Project with ID: " + project.getExternalId());
    }

    try {
      return AwsConnectionUtils.awsConnBeanFromDescriptor(connectionDescriptor, myAwsConnectorFactory, featureProperties);
    } catch (AwsConnectorException awsConnectorException) {
      throw new LinkedAwsConnNotFoundException(
        "Could not get the AWS Credentials: " + awsConnectorException.getMessage() + ". AWS Connection ID: " + awsConnectionId + ". Project ID: " + project.getExternalId());
    }
  }

  @Nullable
  @Override
  public AwsConnectionBean getAwsConnection(@NotNull SProject project, @NotNull String awsConnectionId, Map<String, String> connectionParameters) {
    OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(project, awsConnectionId);
    if (connectionDescriptor == null) {
      return null;
    }

    try {
      return AwsConnectionUtils.awsConnBeanFromDescriptor(connectionDescriptor, myAwsConnectorFactory, connectionParameters);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(String.format("Cannot resolve AWS connection with ID '%s' in project '%s'", awsConnectionId, project.getExternalId()), e);
      return null;
    }
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @Nullable
  @Override
  public AwsConnectionBean getEnvVarAwsConnectionForBuild(@NotNull final SBuild build) throws AwsBuildFeatureException {
    if (build.getBuildId() < 0) {
      throw new AwsBuildFeatureException("Dummy build with negative id " + build.getBuildId() + " does not have AWS Connections to expose");
    }

    SBuildType buildType = build.getBuildType();
    if (buildType == null) {
      throw new AwsBuildFeatureException("There is no BuildType for the Build with id: " + build.getBuildId());
    }

    BuildSettings buildSettings = ((BuildPromotionEx)build.getBuildPromotion()).getBuildSettings();

    SBuildFeatureDescriptor configuredAwsConnBuildFeature;
    try {
      configuredAwsConnBuildFeature = buildSettings.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE).iterator().next();
    } catch (NoSuchElementException noExposeAwsConnBuildFeaturesException) {
      return null;
    }

    return getLinkedAwsConnection(configuredAwsConnBuildFeature.getParameters(), buildType.getProject());
  }
}