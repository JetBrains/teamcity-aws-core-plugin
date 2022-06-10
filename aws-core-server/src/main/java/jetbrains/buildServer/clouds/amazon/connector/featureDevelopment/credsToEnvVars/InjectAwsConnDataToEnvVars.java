package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InjectAwsConnDataToEnvVars implements BuildStartContextProcessor, PasswordsProvider {

  private final AwsConnectionsManager myAwsConnectionsManager;

  public InjectAwsConnDataToEnvVars(@NotNull final AwsConnectionsManager awsConnectionsManager) {
    myAwsConnectionsManager = awsConnectionsManager;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    try {
      AwsConnectionBean awsConnection = myAwsConnectionsManager.getAwsConnectionForBuild(context.getBuild());
      getConnectionParametersToExpose(awsConnection)
        .forEach(context::addSharedParameter);

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Failed to expose AWS Connection to a build: " + e.getMessage());
    }
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull SBuild build) {
    try {
      AwsConnectionBean awsConnection = myAwsConnectionsManager.getAwsConnectionForBuild(build);
      ArrayList<Parameter> secureParams = new ArrayList<>();

      getConnectionParametersToExpose(awsConnection)
        .forEach((k,v) -> secureParams.add(new SimpleParameter(k, v)));

      return secureParams;

    } catch (AwsBuildFeatureException e) {
      Loggers.CLOUD.warn("Failed to expose AWS Connection to a build: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  private Map<String, String> getConnectionParametersToExpose(AwsConnectionBean awsConnection){
    Map<String, String> parameters = new HashMap<>();

    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT, awsConnection.getRegion());

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();
    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, credentials.getAccessKeyId());
    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, credentials.getSecretAccessKey());

    if (awsConnection.isUsingSessionCredentials()) {
      if (credentials.getSessionToken() == null)
        Loggers.CLOUD.warn("Something wrong with the session credentials, the session token is null when session credentials were used.");
      parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT, credentials.getSessionToken());
    }

    return parameters;
  }
}
