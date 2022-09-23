package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.AwsBuildFeatureException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import org.jetbrains.annotations.NotNull;

public class InjectAwsCredentialsToTheBuildContext implements BuildStartContextProcessor {

  private final AwsConnectionsManager myAwsConnectionsManager;

  public InjectAwsCredentialsToTheBuildContext(@NotNull final AwsConnectionsManager awsConnectionsManager) {
    myAwsConnectionsManager = awsConnectionsManager;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    if (!AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(context.getBuild()).isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to expose, getting AWS Connection...", context.getBuild().getBuildId()));

      try {
        AwsConnectionBean awsConnection = myAwsConnectionsManager.getEnvVarAwsConnectionForBuild(context.getBuild());
        if (awsConnection == null) {
          String message = String.format("Feature \"%s\" enabled for the build, but there is no suitable AWS connection configured", AwsConnToAgentBuildFeature.DISPLAY_NAME);
          context.getBuild().getBuildLog()
                 .message(
                   message,
                   Status.WARNING,
                   MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
                 );
          return;
        }
        Map<String, String> parameters = getConnectionParametersToExpose(awsConnection);
        String encodedCredentials = parametersToEncodedString(parameters);

        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, parameters.get(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT));
        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, encodedCredentials);
        context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT, awsConnection.getRegion());
      } catch (AwsBuildFeatureException e) {
        String warningMessage = "Failed to expose AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warn(warningMessage);
        context.getBuild().getBuildLog()
               .message(
                 warningMessage,
                 Status.WARNING,
                 MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(warningMessage))
               );
      }
    }
  }

  @NotNull
  private String parametersToEncodedString(@NotNull Map<String, String> parameters) {
    String prefix = "[default]" + "\n";
    String credentials = parameters.entrySet().stream()
                                   .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                                   .collect(Collectors.joining("\n", prefix, ""));

    return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  @NotNull
  private Map<String, String> getConnectionParametersToExpose(@NotNull final AwsConnectionBean awsConnection) {
    Map<String, String> parameters = new HashMap<>();

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();
    parameters.put(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, credentials.getAccessKeyId());

    addSecureParameters(parameters, awsConnection);
    return parameters;
  }

  private void addSecureParameters(@NotNull final Map<String, String> parameters, @NotNull final AwsConnectionBean awsConnection) {
    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();

    parameters.put(AwsConnBuildFeatureParams.AWS_SECRET_KEY_ENV_PARAM_DEFAULT, credentials.getSecretAccessKey());

    if (awsConnection.isUsingSessionCredentials()) {
      if (credentials.getSessionToken() == null) {
        Loggers.CLOUD.warn("Something is wrong with the session credentials, the session token is null when session credentials were used.");
      }
      parameters.put(AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT, credentials.getSessionToken());
    }
  }
}
