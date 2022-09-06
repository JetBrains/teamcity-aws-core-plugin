package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.DuplicatedAwsConnectionIdException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionsEventsListener extends BuildServerAdapter {

  private final AwsConnectionsHolder myAwsConnectionsHolder;
  private final AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder;

  public AwsConnectionsEventsListener(@NotNull final AwsConnectionsHolder awsConnectionsHolder,
                                      @NotNull final AwsConnectionDescriptorBuilder awsConnectionDescriptorBuilder,
                                      @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher) {
    myAwsConnectionsHolder = awsConnectionsHolder;
    myAwsConnectionDescriptorBuilder = awsConnectionDescriptorBuilder;
    buildServerEventDispatcher.addListener(this);
  }

  @Override
  public void projectRestored(@NotNull String projectId) {
    myAwsConnectionsHolder.rebuildAllConnectionsForProject(projectId);
  }

  @Override
  public void projectRemoved(@NotNull SProject project) {
    myAwsConnectionsHolder.removeAllConnectionsForProject(project);
  }

  @Override
  public void serverShutdown() {
    //TODO: TW-77164 cancel all connections refreshing tasks
    myAwsConnectionsHolder.clear();
  }

  @Override
  public void projectFeatureAdded(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor projectFeature) {
    if (!isAwsConnectionFeature(projectFeature)) {
      return;
    }
    //TODO TW-77164 Add refreshing task
    try {
      AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionDescriptorBuilder.fromFeatureDescriptor(projectFeature);
      myAwsConnectionsHolder.addAwsConnection(awsConnectionDescriptor);
      Loggers.CLOUD.debug(String.format("Added AWS Connection '%s' in the Project with ID: '%s'", projectFeature.getId(), project.getExternalId()));

    } catch (DuplicatedAwsConnectionIdException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format("Can not add AWS Connection '%s' in the Project with ID: '%s', the AWS Connection ID is duplicated", projectFeature.getId(), project.getExternalId()), e);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format("Can not create AWS Connection '%s' in the Project with ID: '%s', reason: <%s>", projectFeature.getId(), project.getExternalId(), e.getMessage()), e);
    }
  }

  @Override
  public void projectFeatureChanged(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor before, @NotNull final SProjectFeatureDescriptor after) {
    if (!isAwsConnectionFeature(after)) {
      return;
    }
    //TODO: TW-77164 update the refresher task
    try {
      AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionDescriptorBuilder.fromFeatureDescriptor(after);
      myAwsConnectionsHolder.updateAwsConnection(awsConnectionDescriptor);

    } catch (DuplicatedAwsConnectionIdException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format("Can not update AWS Connection '%s' in the Project with ID: '%s', the AWS Connection ID is duplicated, previous connection ID: '%s'", after.getId(),
                      project.getExternalId(), before.getId()), e);
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(
        String.format("Can not update AWS Connection '%s' in the Project with ID: '%s', reason: <%s>, previous connection ID: '%s'", after.getId(), project.getExternalId(),
                      e.getMessage(), before.getId()), e);
    }
    Loggers.CLOUD.debug(
      String.format("Updated AWS Connection '%s' in the Project with ID: '%s', previous connection ID: '%s'", after.getId(), project.getExternalId(), before.getId()));
  }

  @Override
  public void projectFeatureRemoved(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor projectFeature) {
    if (!isAwsConnectionFeature(projectFeature)) {
      return;
    }
    myAwsConnectionsHolder.removeAwsConnection(projectFeature.getId());
    Loggers.CLOUD.debug(String.format("Removed AWS Connection '%s' in the Project with ID: '%s'", projectFeature.getId(), project.getExternalId()));
  }

  private boolean isAwsConnectionFeature(SProjectFeatureDescriptor projectFeature) {
    if (!OAuthConstants.FEATURE_TYPE.equals(projectFeature.getType())) {
      return false;
    }
    String providerType = projectFeature.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
    return providerType != null && providerType.equals(AwsConnectionProvider.TYPE);
  }
}
