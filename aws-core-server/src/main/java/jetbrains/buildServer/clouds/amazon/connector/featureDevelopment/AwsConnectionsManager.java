package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.Map;
import java.util.NoSuchElementException;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.NoLinkedAwsConnectionIdParameter;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionsManager {
  private final OAuthConnectionsManager myConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionsManager(@NotNull final OAuthConnectionsManager connectionsManager,
                               @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myConnectionsManager = connectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  /**
   * Will get AWS Connection ID from the properties, find corresponding AWS Connection
   * and return a data bean with all properties like connectionId, providerType and all properties map.
   *
   * @param properties properties Map where should be a parameter with chosen AWS Connection ID.
   * @param project    project which will be searched for the AWS Connection.
   * @return AwsConnectionBean data bean with all AWS Connection properties.
   * @throws NoLinkedAwsConnectionIdParameter thrown when there is no corresponding {@link AwsCloudConnectorConstants#CHOSEN_AWS_CONN_ID_PARAM property} in the properties map.
   */
  @Nullable
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> properties, @NotNull final SProject project) throws NoLinkedAwsConnectionIdParameter {
    String awsConnectionId = properties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new NoLinkedAwsConnectionIdParameter("AWS Connection ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property.");
    }

    OAuthConnectionDescriptor connectionDescriptor = myConnectionsManager.findConnectionById(project, awsConnectionId);
    if (connectionDescriptor == null) {
      return null;
    }

    AWSCredentialsProvider credentials = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionDescriptor.getParameters());
    return new AwsConnectionBean(connectionDescriptor.getId(),
                                 credentials,
                                 connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM),
                                 ParamUtil.useSessionCredentials(connectionDescriptor.getParameters())
    );
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @Nullable
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