package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.*;

public class InjectAwsCredentialsToTheBuildContext implements BuildStartContextProcessor {
  private final AwsCredentialsInjector myAwsCredentialsInjector;
  @NotNull
  private final LinkedAwsConnectionProvider myLinkedAwsConnectionProvider;

  private final String INJECT_CREDENTIALS_PROBLEM_ID = "InjectAwsCredentials";

  public InjectAwsCredentialsToTheBuildContext(@NotNull final LinkedAwsConnectionProvider linkedAwsConnectionProvider) {
    myAwsCredentialsInjector = new AwsCredentialsInjector();
    myLinkedAwsConnectionProvider = linkedAwsConnectionProvider;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext context) {
    Collection<SBuildFeatureDescriptor> awsCredentialsBuildFeatures = AwsConnToAgentBuildFeature.getAwsConnectionsToExpose(context.getBuild());
    if (!awsCredentialsBuildFeatures.isEmpty()) {
      Loggers.CLOUD.debug(String.format("Build with id: <%s> has AWS Connection to inject, looking for AWS Connection...", context.getBuild().getBuildId()));
      try {
        validateMultipleAwsCredentialsBuildFeatures(awsCredentialsBuildFeatures);

        List<ConnectionCredentials> linkedAwsConnectionCredentials = myLinkedAwsConnectionProvider.getConnectionCredentialsFromBuild(context.getBuild());
        if (linkedAwsConnectionCredentials.isEmpty()) {
          finishBuildWithProblem(context, "Cannot access AWS connection(s) used in this build via AWS Credentials Build Feature. Check connection(s) settings and ensure they are shared with child subprojects and/or available for build steps.");
          return;
        }

        for (ConnectionCredentials connectionCredentials : linkedAwsConnectionCredentials) {
          String awsProfileName = connectionCredentials.getProperties().get(AWS_PROFILE_NAME_PARAM);
          myAwsCredentialsInjector.injectCredentials(context, new AwsConnectionCredentials(connectionCredentials), awsProfileName);
        }

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

  private void validateMultipleAwsCredentialsBuildFeatures(@NotNull final Collection<SBuildFeatureDescriptor> awsCredentialsBuildFeatures) throws ConnectionCredentialsException {
    List<String> injectedAwsProfileNames = new ArrayList<>();
    for (SBuildFeatureDescriptor awsCredentialsFeature : awsCredentialsBuildFeatures) {
      String awsProfileName = awsCredentialsFeature.getParameters().get(AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM);
      if (!ParamUtil.isValidAwsProfileName(awsProfileName)) {
        throw new ConnectionCredentialsException(AWS_PROFILE_ERROR);
      }
      injectedAwsProfileNames.add(awsProfileName);
    }
    throwExceptionIfDuplicatedProfileNames(injectedAwsProfileNames);
  }

  private void throwExceptionIfDuplicatedProfileNames(@NotNull final List<String> awsProfileNames) throws ConnectionCredentialsException {
    Map<String, Long> profileNameCounts = awsProfileNames
      .stream()
      .collect(Collectors.groupingBy(
        profileName -> profileName != null ? profileName : "default",
        Collectors.counting()
      ));

    List<String> duplicates = profileNameCounts.entrySet()
      .stream()
      .filter(entry -> entry.getValue() > 1)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());

    if (!duplicates.isEmpty()) {
      String duplicateProfiles = String.join(", ", duplicates);
      throw new ConnectionCredentialsException("There are duplicated AWS Profile names: " + duplicateProfiles);
    }
  }
}
