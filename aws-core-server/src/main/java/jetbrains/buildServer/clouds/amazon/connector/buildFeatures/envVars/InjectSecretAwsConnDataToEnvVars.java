package jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.util.ArrayList;
import java.util.Collection;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.Parameter;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SimpleParameter;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import org.jetbrains.annotations.NotNull;

public class InjectSecretAwsConnDataToEnvVars implements PasswordsProvider {

  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final AwsConnectorFactory myAwsConnectorFactory;

  public InjectSecretAwsConnDataToEnvVars(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                                          @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull SBuild build) {
    ArrayList<Parameter> secureParams = new ArrayList<>();
    ArrayList<OAuthConnectionDescriptor> awsConnections = AwsConnToEnvVarsBuildFeature.getAwsConnections(build, myOAuthConnectionsManager);
    if(awsConnections.size() == 0){
      Loggers.CLOUD.warn("There is no AWS Connection to expose. Check that chosen AWS Connection exists.");
      return new ArrayList<>();
    }

    AWSCredentialsProvider creds = myAwsConnectorFactory.buildAwsCredentialsProvider(awsConnections.get(0).getParameters());

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

    return secureParams;
  }
}
