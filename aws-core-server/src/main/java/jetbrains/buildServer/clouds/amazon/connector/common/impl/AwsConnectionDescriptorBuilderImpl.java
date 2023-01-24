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
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;
import static jetbrains.buildServer.serverSide.connections.utils.ConnectionUtils.getConectionProviderOfType;

public class AwsConnectionDescriptorBuilderImpl implements AwsConnectionDescriptorBuilder {

  private final ExtensionHolder myExtensionHolder;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionDescriptorBuilderImpl(@NotNull final ExtensionHolder extensionHolder,
                                            @NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                            @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myExtensionHolder = extensionHolder;
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
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
      getConectionProviderOfType(myExtensionHolder, AwsConnectionProvider.TYPE),
      myAwsConnectorFactory.describeAwsConnection(featureDescriptor.getParameters())
    );
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull String sessionDuration) throws AwsConnectorException {
    return new AwsConnectionDescriptorImpl(
      featureDescriptor,
      myAwsConnectorFactory.requestNewSessionWithDuration(featureDescriptor, sessionDuration),
      getConectionProviderOfType(myExtensionHolder, AwsConnectionProvider.TYPE),
      myAwsConnectorFactory.describeAwsConnection(featureDescriptor.getParameters())
    );
  }


  @NotNull
  @Override
  @Deprecated
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
      awsConnectionFeature.getConnectionProvider().getType(),
      awsConnectionFeature.getParameters(),
      awsConnectionFeature.getProject().getProjectId()
    );
  }
}
