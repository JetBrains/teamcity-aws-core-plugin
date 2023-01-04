package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.ChosenAwsConnPropertiesProcessor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;

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
        SBuildFeatureDescriptor awsConnectionBuildFeature = myAwsConnectionsManager.getAwsConnectionFeatureFromBuild(context.getBuild());
        if (awsConnectionBuildFeature == null) {
          String message = String.format("Feature \"%s\" enabled for the build, but there is no suitable AWS connection configured", AwsConnToAgentBuildFeature.DISPLAY_NAME);
          context.getBuild().getBuildLog()
                 .messageAsync(
                   message,
                   Status.WARNING,
                   MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
                 );
          return;
        }
        Map<String, String> connectionBuildFeatureProps = awsConnectionBuildFeature.getParameters();

        ChosenAwsConnPropertiesProcessor awsConnsPropertiesProcessor = new ChosenAwsConnPropertiesProcessor();
        Collection<InvalidProperty> invalidProps = awsConnsPropertiesProcessor.process(connectionBuildFeatureProps);
        if (!invalidProps.isEmpty()) {
          InvalidProperty invalidProperty = invalidProps.iterator().next();
          String message = String.format("Add AWS Connection BuildFeature problem detected: %s property is not valid, reason: %s", invalidProperty.getPropertyName(), invalidProperty.getInvalidReason());
          context.getBuild().getBuildLog()
                 .messageAsync(
                   message,
                   Status.WARNING,
                   MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
                 );
          return;
        }

        String awsConnectionId = connectionBuildFeatureProps.get(CHOSEN_AWS_CONN_ID_PARAM);
        if (awsConnectionId == null) {
          String message = String.format("Chosen AWS Connection ID is null in the BuildFeature, will not add AWS Connection to the Build");
          context.getBuild().getBuildLog()
                 .messageAsync(
                   message,
                   Status.WARNING,
                   MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
                 );
          return;
        }
        String sessionDuration = connectionBuildFeatureProps.get(SESSION_DURATION_PARAM);
        AwsConnectionDescriptor awsConnection;
        if (sessionDuration != null) {
          awsConnection = myAwsConnectionsManager.buildWithSessionDuration(awsConnectionId, sessionDuration);

        } else {
          awsConnection = myAwsConnectionsManager.getAwsConnection(awsConnectionId);
          String message = String.format("There is no %s param in the BuildFeature, will use Default SessionDuration for the AWS Connection %s", SESSION_DURATION_PARAM, awsConnectionId);
          context.getBuild().getBuildLog()
                 .messageAsync(
                   message,
                   Status.WARNING,
                   MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
                 );
        }

        Map<String, String> parameters = getConnectionParametersToExpose(awsConnection);
        String encodedCredentials = parametersToEncodedString(parameters);

        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, parameters.get(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT));
        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, encodedCredentials);
        context.addSharedParameter(Constants.ENV_PREFIX + AwsConnBuildFeatureParams.AWS_REGION_ENV_PARAM_DEFAULT, awsConnection.getRegion());
      } catch (AwsConnectorException e) {
        String warningMessage = "Failed to expose AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warnAndDebugDetails(warningMessage, e);
        context.getBuild().getBuildLog()
               .messageAsync(
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
  private Map<String, String> getConnectionParametersToExpose(@NotNull final AwsConnectionDescriptor awsConnection) {
    Map<String, String> parameters = new HashMap<>();

    AwsCredentialsHolder credentialsHolder = awsConnection.getAwsCredentialsHolder();
    AwsCredentialsData credentials = credentialsHolder.getAwsCredentials();
    parameters.put(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT, credentials.getAccessKeyId());

    addSecureParameters(parameters, awsConnection);
    return parameters;
  }

  private void addSecureParameters(@NotNull final Map<String, String> parameters, @NotNull final AwsConnectionDescriptor awsConnection) {
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
