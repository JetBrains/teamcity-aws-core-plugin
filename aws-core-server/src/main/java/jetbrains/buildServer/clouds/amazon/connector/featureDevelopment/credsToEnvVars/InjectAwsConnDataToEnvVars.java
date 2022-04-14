package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import org.jetbrains.annotations.NotNull;

public class InjectAwsConnDataToEnvVars implements BuildStartContextProcessor, PasswordsProvider {

  private final AwsConnectionsManager myAwsConnectionsManager;

  public InjectAwsConnDataToEnvVars(@NotNull final AwsConnectionsManager awsConnectionsManager) {
    myAwsConnectionsManager = awsConnectionsManager;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    AwsConnectionBean awsConnection = myAwsConnectionsManager.getAwsConnectionForBuild(context.getBuild());
    if (awsConnection == null) {
      return;
    }

    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT, awsConnection.getRegion());

    AWSCredentialsProvider creds = awsConnection.getCredentialsProvider();
    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSAccessKeyId());
    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, creds.getCredentials().getAWSSecretKey());

    if (creds.getCredentials() instanceof BasicSessionCredentials) {
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT,
                                 ((BasicSessionCredentials)creds.getCredentials()).getSessionToken());
    }
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull SBuild build) {
    AwsConnectionBean awsConnection = myAwsConnectionsManager.getAwsConnectionForBuild(build);
    if (awsConnection == null) {
      return Collections.emptyList();
    }

    AWSCredentialsProvider creds = awsConnection.getCredentialsProvider();

    ArrayList<Parameter> secureParams = new ArrayList<>();
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
