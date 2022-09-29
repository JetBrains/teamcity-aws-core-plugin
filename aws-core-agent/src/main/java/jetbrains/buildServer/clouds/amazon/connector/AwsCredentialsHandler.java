package jetbrains.buildServer.clouds.amazon.connector;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class AwsCredentialsHandler extends AgentLifeCycleAdapter {
  private static final Logger LOG = Logger.getInstance(AwsCredentialsHandler.class);
  public static final String AWS_CREDENTIALS_FILE_NAME = "aws.credentials";
  public static final String EMPTY_STRING = "";
  private byte[] myCredentialsData;

  public AwsCredentialsHandler(@NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
    agentDispatcher.addListener(this);
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    String encodedCredentials = runningBuild.getSharedConfigParameters()
                                            .get(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT);
    String awsAccessKey = runningBuild.getSharedConfigParameters()
                                      .get(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_ENV_PARAM_DEFAULT);

    if (Strings.isBlank(encodedCredentials)) {
      LOG.debug("Encoded AWS credentials were not provided with this build");
      return;
    }

    try {
      myCredentialsData = Base64.getDecoder().decode(encodedCredentials);
      runningBuild.getBuildLogger().logMessage(DefaultMessagesInfo.createTextMessage("Using AWS Access key: " + awsAccessKey));
      runningBuild.getBuildLogger().logMessage(DefaultMessagesInfo.createTextMessage("AWS credentials are successfully associated with this build"));
    } catch (Exception e) {
      String msg = "Parsing of AWS credentials failed. Error: " + e.getMessage();
      LOG.warn(msg, e);
      runningBuild.getBuildLogger().warning(msg);
    } finally {
      runningBuild.addSharedConfigParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, EMPTY_STRING);
    }
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    if (myCredentialsData == null) {
      return;
    }

    try {
      final File awsCredentialsFile = createFileInTempDirectory(runner.getBuild());

      try (FileOutputStream os = new FileOutputStream(awsCredentialsFile)) {
        os.write(myCredentialsData);
      }

      runner.addEnvironmentVariable(
        AwsConnBuildFeatureParams.AWS_SHARED_CREDENTIALS_FILE,
        awsCredentialsFile.getAbsolutePath()
      );

    } catch (Exception e) {
      String msg = "Failed to create temporary file for AWS credentials. Error: " + e.getMessage();
      LOG.warn(msg, e);
      runner.getBuild().getBuildLogger().warning(msg);
    }
  }

  @Override
  public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    myCredentialsData = null;
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
}
