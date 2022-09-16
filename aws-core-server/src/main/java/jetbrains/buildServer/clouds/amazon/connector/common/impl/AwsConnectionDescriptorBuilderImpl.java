package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
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

  public AwsConnectionDescriptorBuilderImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                            @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildFromProject(@NotNull final SProject project, @NotNull final String awsFeatureConnectionId) throws AwsConnectorException {
    OAuthConnectionDescriptor awsConnectionFeature = myOAuthConnectionsManager.findConnectionById(project, awsFeatureConnectionId);
    if (awsConnectionFeature == null) {
      throw new AwsConnectorException("There is no AWS Connection with ID: " + awsFeatureConnectionId + " in the project with ID: " + project.getExternalId());
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
      myAwsConnectorFactory.describeAwsConnection(featureDescriptor.getParameters())
    );
  }

  @NotNull
  @Override
  public AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull String sessionDuration) throws AwsConnectorException {
    //TODO: TW-77164 When factory will return only configured object, WITHOUT auto-refreshing, just use normal method, but with specified session duration
    return new AwsConnectionDescriptorImpl(
      featureDescriptor,
      myAwsConnectorFactory.requestNewSessionWithDuration(featureDescriptor, sessionDuration),
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
      awsConnectionFeature.getOauthProvider().getType(),
      awsConnectionFeature.getParameters(),
      awsConnectionFeature.getProject().getProjectId()
    );
  }
}
