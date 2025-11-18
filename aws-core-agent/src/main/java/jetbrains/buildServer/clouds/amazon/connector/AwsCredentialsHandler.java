package jetbrains.buildServer.clouds.amazon.connector;

import com.intellij.openapi.diagnostic.Logger;

import com.intellij.openapi.util.io.StreamUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import java.util.List;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.getAwsProfileNameOrDefault;

public class AwsCredentialsHandler extends AgentLifeCycleAdapter {
  private static final Logger LOG = Logger.getInstance(AwsCredentialsHandler.class);
  public static final String AWS_CREDENTIALS_FILE_NAME = "aws.credentials";
  public static final String EMPTY_STRING = "";

  private String myAwsProfileNames;
  private byte[] myCredentialsData;

  public AwsCredentialsHandler(@NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
    agentDispatcher.addListener(this);
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    String encodedCredentials = runningBuild.getSharedConfigParameters()
                                            .get(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT);

    String awsAccessKeys = runningBuild.getSharedConfigParameters()
                                       .get(AwsConnBuildFeatureParams.INJECTED_AWS_ACCESS_KEYS);

    myAwsProfileNames = runningBuild.getSharedConfigParameters()
                                    .get(AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM);

    if (Strings.isBlank(encodedCredentials)) {
      return;
    }
    LOG.debug(String.format("Encoded AWS credentials were provided in build with id <%s>", runningBuild.getBuildId()));
    runningBuild.getBuildLogger().logMessage(
      DefaultMessagesInfo.createTextMessage(String.format("Found AWS Credentials with Access Key ID(s): %s and AWS Profile name(s): %s", awsAccessKeys, myAwsProfileNames)));

    try {
      myCredentialsData = Base64.getDecoder().decode(encodedCredentials);
      maskCredentialsSecrets(runningBuild);
    } catch (Exception e) {
      String msg = "Parsing of AWS credentials failed. Error: " + e.getMessage();
      LOG.warn(msg, e);
      runningBuild.getBuildLogger().warning(msg);
    } finally {
      runningBuild.addSharedConfigParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, EMPTY_STRING);
    }
  }

  private void maskCredentialsSecrets(@NotNull AgentRunningBuild runningBuild) {
    try {
      final String textStream = StreamUtil.readText(new ByteArrayInputStream(myCredentialsData));
      final List<String> textByLines = Arrays.asList(textStream.split("\\r?\\n"));
      // Ignoring the profile name line prefix
      for (String line : textByLines) {
        if (line.contains(AwsConnBuildFeatureParams.AWS_SECRET_KEY_CONFIG_FILE_PARAM) || line.contains(AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_CONFIG_FILE_PARAM)) {
          final String secretAccessKey = line.substring(line.indexOf("=") + 1);
          runningBuild.getPasswordReplacer().addPassword(secretAccessKey);
        }
      }
    } catch (IOException e) {
      LOG.warnAndDebugDetails("Failed to mask AWS credentials secrets", e);
    }
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    if (myCredentialsData == null) {
      return;
    }

    runner.getBuild().getBuildLogger().logMessage(DefaultMessagesInfo.createTextMessage("Creating a file with AWS Credentials..."));
    try {
      final File awsCredentialsFile = createFileInTempDirectory(runner.getBuild());

      try (FileOutputStream os = new FileOutputStream(awsCredentialsFile)) {
        os.write(myCredentialsData);
      }
      runner.getBuild().getBuildLogger().logMessage(DefaultMessagesInfo.createTextMessage("Created the AWS Credentials file"));

      setAwsProfileNameEnvVarForSingleConnectionOnly(runner);

      runner.addEnvironmentVariable(
        AwsConnBuildFeatureParams.AWS_SHARED_CREDENTIALS_FILE_ENV,
        awsCredentialsFile.getAbsolutePath()
      );
      runner.getBuild().getBuildLogger().logMessage(DefaultMessagesInfo.createTextMessage(
        String.format("Added Environment Variable <%s> which points to the AWS Credentials file", AwsConnBuildFeatureParams.AWS_SHARED_CREDENTIALS_FILE_ENV)));

    } catch (Exception e) {
      String msg = "Failed to create temporary file for AWS credentials, reason: " + e.getMessage();
      LOG.warn(msg, e);
      runner.getBuild().getBuildLogger().warning(msg);
    }
  }

  @Override
  public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    myCredentialsData = null;
    myAwsProfileNames = null;
  }

  private File createFileInTempDirectory(@NotNull AgentBuildSettings build) throws IOException {
    //NOTE: Agent temp will be cleaned by one of build stages.
    //NOTE: All we need here is to generate some name for a file that will be
    //NOTE: created later in the SaveProperties to Files build stage

    try {
      //This directory may not exist.
      final File tempDirectory = build.getAgentTempDirectory();

      //We need directory to create temp file name
      tempDirectory.mkdirs();

      File file = new File(tempDirectory, AWS_CREDENTIALS_FILE_NAME);
      if (file.isFile()) {
        FileUtil.delete(file);
      }
      if (!file.createNewFile()) {
        throw new IOException("Can't create file: " + file.getAbsolutePath());
      }

      return FileUtil.getCanonicalFile(file);

    } catch (final IOException e) {
      LOG.warn(e.getMessage(), e);
      throw new IOException(e.getMessage()) {{
        initCause(e);
      }};
    }
  }

  /**
   * TW-77603 If multiple AWS Connections are injected - user should manage the named profiles in scripts or etc, we should not enforce any env var for it
   */
  private void setAwsProfileNameEnvVarForSingleConnectionOnly(@NotNull BuildRunnerContext runner) {
    if (!myAwsProfileNames.contains(", ")) {
      runner.addEnvironmentVariable(
        AwsConnBuildFeatureParams.AWS_PROFILE_NAME_ENV,
        getAwsProfileNameOrDefault(myAwsProfileNames)
      );
    }
  }
}
