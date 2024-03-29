

package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.Map;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectionNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

public class AwsConnectionDescriptorBuilderImpl implements AwsConnectionDescriptorBuilder {


  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;
  private final ExtensionHolder myExtensionHolder;

  public AwsConnectionDescriptorBuilderImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                            @NotNull final AwsConnectorFactory awsConnectorFactory,
                                            @NotNull final ExtensionHolder extensionHolder) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
    myExtensionHolder = extensionHolder;
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildFromProject(@NotNull final SProject project, @NotNull final String awsFeatureConnectionId) throws AwsConnectorException {
    OAuthConnectionDescriptor awsConnectionFeature = myOAuthConnectionsManager.findConnectionById(project, awsFeatureConnectionId);
    if (awsConnectionFeature == null) {
      throw new AwsConnectionNotFoundException(awsFeatureConnectionId);
    }
    return fromFeatureDescriptor(projectFeatureDescriptorFromOauthConn(awsConnectionFeature));
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor fromFeatureDescriptor(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    AwsCredentialsHolder awsCredentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(featureDescriptor);
    return new AwsConnectionDescriptorImpl(
      featureDescriptor,
      awsCredentialsHolder,
      myExtensionHolder
    );
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull String sessionDuration) throws AwsConnectorException {
    return new AwsConnectionDescriptorImpl(
      featureDescriptor,
      myAwsConnectorFactory.requestNewSessionWithDuration(featureDescriptor, sessionDuration),
      myExtensionHolder
    );
  }


  @NotNull
  @Override
  public AwsConnectionBean awsConnBeanFromDescriptor(@NotNull final AwsConnectionDescriptor connectionDescriptor,
                                                     @NotNull final Map<String, String> connectionParameters)
    throws AwsConnectorException {

    AwsCredentialsHolder credentialsHolder;
    String sessionDuration = connectionParameters.get(SESSION_DURATION_PARAM);
    if (sessionDuration != null) {
      credentialsHolder = myAwsConnectorFactory
        .requestNewSessionWithDuration(connectionDescriptor, sessionDuration);

    } else {
      credentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionDescriptor);
    }

    return new AwsConnectionBean(
      connectionDescriptor.getId(),
      myAwsConnectorFactory.describeAwsConnection(connectionDescriptor.getParameters()),
      credentialsHolder,
      connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM)
    );
  }

  @NotNull
  private SProjectFeatureDescriptor projectFeatureDescriptorFromOauthConn(@NotNull final OAuthConnectionDescriptor awsConnectionFeature) {
    return new ProjectFeatureDescriptorImpl(
      awsConnectionFeature.getId(),
      awsConnectionFeature.getOauthProvider().getType(),
      awsConnectionFeature.getParameters(),
      awsConnectionFeature.getProject().getProjectId()
    );
  }
}