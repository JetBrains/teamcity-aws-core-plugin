package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentials;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentials credentials = credentialsHolder.getAwsCredentials();

    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, credentials.getAccessKeyId());
    context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, credentials.getSecretAccessKey());

    if (awsConnection.isUsingSessionCredentials()) {
      if (credentials.getSessionToken() == null) {
        Loggers.CLOUD.warn("Something wrong with the session credentials, the session token is null when session credentials were used.");
      }
      context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT, credentials.getSessionToken());
    }
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull SBuild build) {
    AwsConnectionBean awsConnection = myAwsConnectionsManager.getAwsConnectionForBuild(build);
    if (awsConnection == null) {
      return Collections.emptyList();
    }

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentials credentials = credentialsHolder.getAwsCredentials();

    ArrayList<Parameter> secureParams = new ArrayList<>();
    secureParams.add(new SimpleParameter(
      Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT,
      credentials.getSecretAccessKey())
    );

    if (awsConnection.isUsingSessionCredentials()) {
      if (credentials.getSessionToken() == null) {
        Loggers.CLOUD.warn("Something wrong with the session credentials, the session token is null when session credentials were used.");
      }
      secureParams.add(new SimpleParameter(
        Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT,
        credentials.getSessionToken())
      );
    }

    return secureParams;
  }
}
