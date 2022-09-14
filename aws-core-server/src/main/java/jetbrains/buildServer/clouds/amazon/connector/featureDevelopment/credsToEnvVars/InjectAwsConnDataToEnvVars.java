package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import java.util.*;
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

public class InjectAwsConnDataToEnvVars implements BuildStartContextProcessor, PasswordsProvider {

  private final AwsConnectionsManager myAwsConnectionsManager;

  public InjectAwsConnDataToEnvVars(@NotNull final AwsConnectionsManager awsConnectionsManager) {
    myAwsConnectionsManager = awsConnectionsManager;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    if (!AwsConnToEnvVarsBuildFeature.getAwsConnectionsToExpose(context.getBuild()).isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to expose, getting AWS Connection...", context.getBuild().getBuildId()));

      try {
        AwsConnectionBean awsConnection = myAwsConnectionsManager.getEnvVarAwsConnectionForBuild(context.getBuild());
        if (awsConnection == null) {
          return;
        }
        getConnectionParametersToExpose(awsConnection)
          .forEach(context::addSharedParameter);
      } catch (AwsBuildFeatureException e) {
        Loggers.CLOUD.warn("Failed to expose AWS Connection to a build: " + e.getMessage());
      }
    }
  }

  @NotNull
  private Map<String, String> getConnectionParametersToExpose(@NotNull final AwsConnectionBean awsConnection) {
    Map<String, String> parameters = new HashMap<>();

    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT, awsConnection.getRegion());

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();
    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, credentials.getAccessKeyId());

    addSecureParameters(parameters, awsConnection);
    return parameters;
  }

  @NotNull
  @Override
  public Collection<Parameter> getPasswordParameters(@NotNull final SBuild build) {
    ArrayList<Parameter> secureParams = new ArrayList<>();
    Map<String, String> secureParamsMap = new HashMap<>();

    if (!AwsConnToEnvVarsBuildFeature.getAwsConnectionsToExpose(build).isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to expose, getting AWS Connection...", build.getBuildId()));

      try {
        AwsConnectionBean awsConnection = myAwsConnectionsManager.getEnvVarAwsConnectionForBuild(build);
        if (awsConnection == null) {
          return Collections.emptyList();
        }
        addSecureParameters(secureParamsMap, awsConnection);

      } catch (AwsBuildFeatureException e) {
        Loggers.CLOUD.warn("Failed to expose AWS Connection to a build: " + e.getMessage());
      }
    }

    secureParamsMap.forEach((k, v) -> secureParams.add(new SimpleParameter(k, v)));
    return secureParams;
  }

  private void addSecureParameters(@NotNull final Map<String, String> parameters, @NotNull final AwsConnectionBean awsConnection) {
    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();

    parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, credentials.getSecretAccessKey());

    if (awsConnection.isUsingSessionCredentials()) {
      if (credentials.getSessionToken() == null) {
        Loggers.CLOUD.warn("Something is wrong with the session credentials, the session token is null when session credentials were used.");
      }
      parameters.put(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT, credentials.getSessionToken());
    }
  }
}
