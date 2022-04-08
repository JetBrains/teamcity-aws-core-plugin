package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.util.List;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.buildFeatures.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.util.StringUtil.emptyIfNull;

public class InjectAwsConnDataToEnvVars implements BuildStartContextProcessor {

  private final AwsConnectionsManager myAwsConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public InjectAwsConnDataToEnvVars(@NotNull final AwsConnectionsManager awsConnectionsManager,
                                    @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectionsManager = awsConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    List<AwsConnectionBean> awsConnections = myAwsConnectionsManager.getAwsConnectionsForBuild(context.getBuild());

    for (AwsConnectionBean awsConnection : awsConnections) {
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT,
                                 emptyIfNull(awsConnection.getProperties().get(AwsCloudConnectorConstants.REGION_NAME_PARAM)));

      AWSCredentialsProvider creds = myAwsConnectorFactory.buildAwsCredentialsProvider(awsConnection.getProperties());
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSAccessKeyId());
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSSecretKey());

      if (creds.getCredentials() instanceof BasicSessionCredentials) {
        context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT,
                                   ((BasicSessionCredentials)creds.getCredentials()).getSessionToken());
      }
    }
  }
}
