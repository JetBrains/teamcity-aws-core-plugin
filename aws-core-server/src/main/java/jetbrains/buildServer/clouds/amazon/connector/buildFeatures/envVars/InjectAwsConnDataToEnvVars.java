package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.util.ArrayList;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.util.StringUtil.emptyIfNull;

public class InjectAwsConnDataToEnvVars implements BuildStartContextProcessor {

  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public InjectAwsConnDataToEnvVars(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                    @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    //TODO: add support for several AWS Connections exposing
    ArrayList<OAuthConnectionDescriptor> awsConnections = AwsConnToEnvVarsBuildFeature.getAwsConnections(context.getBuild(), myOAuthConnectionsManager);
    if (awsConnections.size() == 0) {
      Loggers.CLOUD.warn("There is no AWS Connection to expose. Check that chosen AWS Connection exists.");
      return;
    }

    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT,
                               emptyIfNull(awsConnections.get(0).getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM)));

    AWSCredentialsProvider creds = myAwsConnectorFactory.buildAwsCredentialsProvider(awsConnections.get(0).getParameters());
    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSAccessKeyId());
    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSSecretKey());

    if (creds.getCredentials() instanceof BasicSessionCredentials) {
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT,
                                 ((BasicSessionCredentials)creds.getCredentials()).getSessionToken());
    }
  }
}
