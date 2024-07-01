package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.*;

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
    Collection<SBuildFeatureDescriptor> awsConnectionsToExpose = AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(context.getBuild());
    if (!awsConnectionsToExpose.isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to inject, looking for AWS Connection...", context.getBuild().getBuildId()));
      final SBuildFeatureDescriptor awsCredentialsBuildFeature = awsConnectionsToExpose.stream().findFirst().get();
      try {
        List<ConnectionCredentials> linkedAwsConnectionCredentials = myLinkedAwsConnectionProvider.getConnectionCredentialsFromBuild(context.getBuild());
        if (linkedAwsConnectionCredentials.isEmpty()) {
          final String errorMessage = getUnavailableConnectionErrorMessage(awsCredentialsBuildFeature);
          finishBuildWithProblem(context, errorMessage);
          return;
        }

        //TODO: TW-75618 Add support for several AWS Connections exposing
        final ConnectionCredentials firstCredentials = linkedAwsConnectionCredentials.stream().findFirst().get();
        AwsConnectionCredentials credentials = new AwsConnectionCredentials(firstCredentials);

        String awsProfileName = awsCredentialsBuildFeature.getParameters().get(AWS_PROFILE_NAME_PARAM);
        if(!ParamUtil.isValidAwsProfileName(awsProfileName)){
          throw new ConnectionCredentialsException(AWS_PROFILE_ERROR);
        }

        myAwsCredentialsInjector.injectCredentials(context, credentials, awsProfileName);
      } catch (ConnectionCredentialsException e) {
        String warningMessage = "Failed to inject AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warnAndDebugDetails(warningMessage, e);
        finishBuildWithProblem(context, warningMessage);
      }
    }
  }

  private static String getUnavailableConnectionErrorMessage(SBuildFeatureDescriptor awsCredentialsBuildFeature) {
    final String awsConnectionName = awsCredentialsBuildFeature.getParameters().get(AwsCloudConnectorConstants.AWS_CONN_DISPLAY_NAME_PARAM);

    final String errorMessage;
    if (!StringUtil.isEmpty(awsConnectionName)){
      errorMessage = String.format("Cannot access the '%s' AWS connection. Check connection settings and ensure it is shared with child subprojects and/or available for build steps.", awsConnectionName);
    } else {
      errorMessage = String.format("Cannot access the AWS connection used in this build. Check connection settings and ensure it is shared with child subprojects and/or available for build steps.");
    }
    return errorMessage;
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
