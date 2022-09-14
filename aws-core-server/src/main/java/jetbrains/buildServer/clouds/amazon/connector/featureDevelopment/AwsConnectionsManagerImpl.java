package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars.AwsConnToEnvVarsBuildFeature;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionsManagerImpl implements AwsConnectionsManager {

  private final AwsConnectionsHolder myAwsConnectionsHolder;
  private final AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder;

  public AwsConnectionsManagerImpl(@NotNull final AwsConnectionsHolder awsConnectionsHolder,
                                   @NotNull final AwsConnectionDescriptorBuilder awsConnectionDescriptorBuilder) {
    myAwsConnectionsHolder = awsConnectionsHolder;
    myAwsConnectionDescriptorBuilder = awsConnectionDescriptorBuilder;
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor getLinkedAwsConnection(@NotNull final Map<String, String> featureProperties) throws LinkedAwsConnNotFoundException {
    String awsConnectionId = featureProperties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new LinkedAwsConnNotFoundException("AWS Connection ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property");
    }
    try {
      return myAwsConnectionsHolder.getAwsConnection(awsConnectionId);

    } catch (AwsConnectorException awsConnectorException) {
      throw new LinkedAwsConnNotFoundException(
        "Could not get the AWS Credentials for the connection with ID '" + awsConnectionId + "', reason: " + awsConnectorException.getMessage()
      );
    }
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor getAwsConnection(@NotNull String awsConnectionId) throws AwsConnectorException {
    return myAwsConnectionsHolder.getAwsConnection(awsConnectionId);
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildWithSessionDuration(@NotNull String awsConnectionId, @NotNull String sessionDuration) throws AwsConnectorException {
    AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionsHolder.getAwsConnection(awsConnectionId);
    return myAwsConnectionDescriptorBuilder.buildWithSessionDuration(awsConnectionDescriptor, sessionDuration);
  }

  //TODO: TW-75618 Add support for several AWS Connections exposing
  @Nullable
  @Override
  public AwsConnectionDescriptor getAwsConnectionFromBuildEnvVar(@NotNull SBuild build) throws AwsBuildFeatureException {
    if (build.getBuildId() < 0) {
      throw new AwsBuildFeatureException("Dummy build with negative id " + build.getBuildId() + " does not have AWS Connections to expose");
    }

    SBuildType buildType = build.getBuildType();
    if (buildType == null) {
      throw new AwsBuildFeatureException("There is no BuildType for the Build with id: " + build.getBuildId());
    }

    Collection<SBuildFeatureDescriptor> awsConnectionsToExpose = AwsConnToEnvVarsBuildFeature.getAwsConnectionsToExpose(build);
    if (awsConnectionsToExpose.isEmpty()) {
      return null;
    }
    SBuildFeatureDescriptor configuredAwsConnBuildFeature = awsConnectionsToExpose.iterator().next();
    return getLinkedAwsConnection(configuredAwsConnBuildFeature.getParameters());
  }


  @NotNull
  @Override
  @Deprecated
  public AwsConnectionBean getLinkedAwsConnection(@NotNull final Map<String, String> featureProperties, @NotNull final SProject project) throws LinkedAwsConnNotFoundException {
    String awsConnectionId = featureProperties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
    if (awsConnectionId == null) {
      throw new LinkedAwsConnNotFoundException(
        "AWS Connection ID was not specified in " + AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM + " property. Project ID: " + project.getExternalId());
    }
    try {
      AwsConnectionDescriptor connectionDescriptor = myAwsConnectionsHolder.getAwsConnection(awsConnectionId);
      return myAwsConnectionDescriptorBuilder.awsConnBeanFromDescriptor(connectionDescriptor, featureProperties);
    } catch (AwsConnectorException e) {
      throw new LinkedAwsConnNotFoundException(
        "Could not find the AWS Connection with ID " + awsConnectionId + " in Project with ID: " + project.getExternalId() + ", more info: " + e.getMessage());
    }
  }

  @Nullable
  @Override
  @Deprecated
  public AwsConnectionBean getAwsConnection(@NotNull SProject project, @NotNull String awsConnectionId, Map<String, String> connectionParameters) {
    try {
      AwsConnectionDescriptor connectionDescriptor = myAwsConnectionsHolder.getAwsConnection(awsConnectionId);
      return myAwsConnectionDescriptorBuilder.awsConnBeanFromDescriptor(connectionDescriptor, connectionParameters);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(String.format("Cannot resolve AWS connection with ID '%s' in project '%s'", awsConnectionId, project.getExternalId()), e);
      return null;
    }
  }

  @Nullable
  @Override
  @Deprecated
  public AwsConnectionBean getEnvVarAwsConnectionForBuild(@NotNull final SBuild build) throws AwsBuildFeatureException {
    if (build.getBuildId() < 0) {
      throw new AwsBuildFeatureException("Dummy build with negative id " + build.getBuildId() + " does not have AWS Connections to expose");
    }

    SBuildType buildType = build.getBuildType();
    if (buildType == null) {
      throw new AwsBuildFeatureException("There is no BuildType for the Build with id: " + build.getBuildId());
    }

    Collection<SBuildFeatureDescriptor> awsConnectionsToExpose = AwsConnToEnvVarsBuildFeature.getAwsConnectionsToExpose(build);
    if (awsConnectionsToExpose.isEmpty()) {
      return null;
    }
    SBuildFeatureDescriptor configuredAwsConnBuildFeature = awsConnectionsToExpose.iterator().next();
    return getLinkedAwsConnection(configuredAwsConnBuildFeature.getParameters(), buildType.getProject());
  }
}