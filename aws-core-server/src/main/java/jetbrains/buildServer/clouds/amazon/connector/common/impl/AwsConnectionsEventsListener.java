package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.isAwsConnectionFeature;

/**
 * All AWS Connections-related management logic is in the {@link AwsConnectionCredentialsFactory}.
 * @deprecated Use {@link jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager} to retrieve credentials.
 */
@Deprecated
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
    myAwsConnectionsHolder.clear();
  }

  @Override
  public void projectFeatureAdded(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor projectFeature) {
    if (!isAwsConnectionFeature(projectFeature)) {
      return;
    }

    AwsConnectionsLogger awsConnectionsLogger = new AwsConnectionsLogger(project);
    try {
      myAwsConnectionsHolder.putDataStorageValue(projectFeature.getId(), projectFeature.getProjectId());
      AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionDescriptorBuilder.fromFeatureDescriptor(projectFeature);
      myAwsConnectionsHolder.addAwsConnection(awsConnectionDescriptor);
      awsConnectionsLogger.connectionAdded(awsConnectionDescriptor.getId());

    } catch (AwsConnectorException e) {
      awsConnectionsLogger.failedToAdd(projectFeature.getId(), e);
    }
  }

  @Override
  public void projectFeatureChanged(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor before, @NotNull final SProjectFeatureDescriptor after) {
    AwsConnectionsLogger awsConnectionsLogger = new AwsConnectionsLogger(project);
    if (!isAwsConnectionFeature(after)) {
      if (isAwsConnectionFeature(before)) {
        myAwsConnectionsHolder.removeAwsConnection(before.getId());
        awsConnectionsLogger.connectionRemoved(before.getId());
      }
      return;
    }

    try {
      myAwsConnectionsHolder.putDataStorageValue(after.getId(), after.getProjectId());
      AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionDescriptorBuilder.fromFeatureDescriptor(after);
      myAwsConnectionsHolder.updateAwsConnection(awsConnectionDescriptor);
      awsConnectionsLogger.connectionUpdated(after.getId());

    } catch (AwsConnectorException e) {
      awsConnectionsLogger.failedToUpdate(after.getId(), e);
    }
  }

  @Override
  public void projectFeatureRemoved(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor projectFeature) {
    if (!isAwsConnectionFeature(projectFeature)) {
      return;
    }
    myAwsConnectionsHolder.removeAwsConnection(projectFeature.getId());
    new AwsConnectionsLogger(project)
      .connectionRemoved(projectFeature.getId());
  }
}
