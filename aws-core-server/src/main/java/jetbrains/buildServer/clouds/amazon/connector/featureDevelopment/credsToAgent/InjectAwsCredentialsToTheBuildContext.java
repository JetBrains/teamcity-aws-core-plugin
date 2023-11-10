package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.List;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

public class InjectAwsCredentialsToTheBuildContext implements BuildStartContextProcessor {
  private final AwsCredentialsInjector myAwsCredentialsInjector;
  @NotNull private final LinkedAwsConnectionProvider myLinkedAwsConnectionProvider;

  private final String INJECT_CREDENTIALS_PROBLEM_ID = "InjectAwsCredentials";

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
          finishBuildWithProblem(context, String.format("Build has AWS Connection to inject, but none was added. Enable debug to see more information."));
          return;
        }

        //TODO: TW-75618 Add support for several AWS Connections exposing
        final ConnectionCredentials firstCredentials = linkedAwsConnectionCredentials.stream().findFirst().get();
        AwsConnectionCredentials credentials = new AwsConnectionCredentials(firstCredentials);

        myAwsCredentialsInjector.injectCredentials(context, credentials);
      } catch (ConnectionCredentialsException e) {
        String warningMessage = "Failed to inject AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warnAndDebugDetails(warningMessage, e);
        finishBuildWithProblem(context, warningMessage);
      }
    }
  }

  private void finishBuildWithProblem(@NotNull BuildStartContext context, @NotNull String message) {
    final SRunningBuild build = context.getBuild();
    build.addBuildProblem(
      BuildProblemData.createBuildProblem(
        INJECT_CREDENTIALS_PROBLEM_ID,
        INJECT_CREDENTIALS_PROBLEM_ID,
        message
      )
    );
    build.stop(null, message);
  }
}
