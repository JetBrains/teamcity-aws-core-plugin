package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.List;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

public class InjectAwsCredentialsToTheBuildContext implements BuildStartContextProcessor {
  private final AwsCredentialsInjector myAwsCredentialsInjector;
  @NotNull private final LinkedAwsConnectionProvider myLinkedAwsConnectionProvider;

  public InjectAwsCredentialsToTheBuildContext(@NotNull final LinkedAwsConnectionProvider linkedAwsConnectionProvider) {
    myAwsCredentialsInjector = new AwsCredentialsInjector();
    myLinkedAwsConnectionProvider = linkedAwsConnectionProvider;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    if (!AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(context.getBuild()).isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to inject, looking for AWS Connection...", context.getBuild().getBuildId()));
      try {
        List<ConnectionCredentials> linkedAwsConnectionCredentials = myLinkedAwsConnectionProvider.getConnectionCredentialsFromBuild(context.getBuild());
        if (linkedAwsConnectionCredentials.isEmpty()) {
          reportProblem(context, String.format("Build with id: <%s> has AWS Connection to inject, but none was added. Enable debug to see more information.",
                                               context.getBuild().getBuildId()));
          return;
        }

        //TODO: TW-75618 Add support for several AWS Connections exposing
        final ConnectionCredentials firstCredentials = linkedAwsConnectionCredentials.stream().findFirst().get();
        AwsConnectionCredentials credentials = new AwsConnectionCredentials(firstCredentials);

        myAwsCredentialsInjector.injectCredentials(context, credentials);
      } catch (ConnectionCredentialsException e) {
        String warningMessage = "Failed to inject AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warnAndDebugDetails(warningMessage, e);
        reportProblem(context, warningMessage);
      }
    }
  }

  private void reportProblem(@NotNull BuildStartContext context, @NotNull String message) {
    context
      .getBuild()
      .getBuildLog()
      .messageAsync(
        message,
        Status.WARNING,
        MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
      );
  }
}
