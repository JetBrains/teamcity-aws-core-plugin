package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import org.jetbrains.annotations.NotNull;

public class InjectAwsCredentialsToTheBuildContext implements BuildStartContextProcessor {
  private final AwsCredentialsInjector myAwsCredentialsInjector;

  public InjectAwsCredentialsToTheBuildContext(@NotNull final LinkedAwsConnectionProvider linkedAwsConnectionProvider) {
    myAwsCredentialsInjector = new AwsCredentialsInjector(linkedAwsConnectionProvider);
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    if (!AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(context.getBuild()).isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to inject, looking for AWS Connection...", context.getBuild().getBuildId()));

      myAwsCredentialsInjector.injectCredentials(context);
    }
  }
}
