package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.buildFeatures.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.Parameter;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SimpleParameter;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import org.jetbrains.annotations.NotNull;

public class InjectSecretAwsConnDataToEnvVars implements PasswordsProvider {

  private final AwsConnectionsManager myAwsConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public InjectSecretAwsConnDataToEnvVars(@NotNull final AwsConnectionsManager awsConnectionsManager,
                                          @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectionsManager = awsConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull SBuild build) {
    ArrayList<Parameter> secureParams = new ArrayList<>();
    List<AwsConnectionBean> awsConnections = myAwsConnectionsManager.getAwsConnectionsForBuild(build);

    for (AwsConnectionBean awsConnection : awsConnections) {

      AWSCredentialsProvider creds = myAwsConnectorFactory.buildAwsCredentialsProvider(awsConnection.getProperties());

      secureParams.add(new SimpleParameter(
        Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT,
        creds.getCredentials().getAWSSecretKey())
      );

      if (creds.getCredentials() instanceof BasicSessionCredentials) {
        secureParams.add(new SimpleParameter(
          Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT,
          ((BasicSessionCredentials)creds.getCredentials()).getSessionToken())
        );
      }
    }

    return secureParams;
  }
}
