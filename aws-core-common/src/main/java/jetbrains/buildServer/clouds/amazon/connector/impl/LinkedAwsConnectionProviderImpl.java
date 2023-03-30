package jetbrains.buildServer.clouds.amazon.connector.impl;

import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import org.jetbrains.annotations.NotNull;

/**
 * Register this as a bean in your plugin to manage linked AWS Connection searching
 */
public class LinkedAwsConnectionProviderImpl implements LinkedAwsConnectionProvider {

  private final ProjectManager myProjectManager;
  private final ProjectConnectionCredentialsManager myProjectConnectionCredentialsManager;

  public LinkedAwsConnectionProviderImpl(@NotNull final ProjectManager projectManager,
                                         @NotNull final ProjectConnectionCredentialsManager projectConnectionCredentialsManager) {
    myProjectManager = projectManager;
    myProjectConnectionCredentialsManager = projectConnectionCredentialsManager;
  }

  @NotNull
  @Override
  public ConnectionCredentials getLinkedConnectionCredentials(@NotNull final SProjectFeatureDescriptor featureWithConnectionDescriptor) throws ConnectionCredentialsException {
    String failedMessage =
      String.format("Failed to get the principal AWS connection for AWS Connection <%s> in Project <%s>: ", featureWithConnectionDescriptor.getId(), featureWithConnectionDescriptor.getProjectId());

    SProject project = myProjectManager.findProjectById(featureWithConnectionDescriptor.getProjectId());
    if (project == null) {
      throw new AwsConnectorException(failedMessage + "Cannot find the Project with ID: " + featureWithConnectionDescriptor.getProjectId());
    }

    String principalAwsConnectionId = ParamUtil.getLinkedAwsConnectionId(featureWithConnectionDescriptor.getParameters());
    if (principalAwsConnectionId == null) {
      throw new AwsConnectorException(failedMessage + "There is no principal AWS Connection ID in the connection properties");
    }

    return myProjectConnectionCredentialsManager.requestConnectionCredentials(project, principalAwsConnectionId);
  }
}
