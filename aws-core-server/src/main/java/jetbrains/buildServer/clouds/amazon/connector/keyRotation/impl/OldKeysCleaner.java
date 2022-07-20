package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.LimitExceededException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.ServiceFailureException;
import com.intellij.openapi.util.Pair;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.IamClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

public class OldKeysCleaner {

  private static final String DELETE_OLD_AWS_KEY_TASK_TYPE = "deleteOldAwsKey";
  private static final String TASK_ARG_DIVIDER = "{}";
  private static final TemporalAmount OLD_KEY_PRESERVE_TIME = Duration.ofDays(1);

  private final MultiNodeTasks myMultiNodeTasks;
  private final ServerResponsibility myServerResponsibility;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final ProjectManager myProjectManager;

  public OldKeysCleaner(@NotNull MultiNodeTasks multiNodeTasks,
                        @NotNull final ServerResponsibility serverResponsibility,
                        @NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                        @NotNull final ProjectManager projectManager,
                        @NotNull final IamClientBuilder iamClientBuilder) {
    myMultiNodeTasks = multiNodeTasks;
    myServerResponsibility = serverResponsibility;
    myOAuthConnectionsManager = oAuthConnectionsManager;
    myProjectManager = projectManager;

    myMultiNodeTasks.subscribe(DELETE_OLD_AWS_KEY_TASK_TYPE, new MultiNodeTasks.TaskConsumer() {
      @Override
      public boolean beforeAccept(@NotNull final MultiNodeTasks.PerformingTask task) {
        if (!myServerResponsibility.canWriteToConfigDirectory() ||
            !DELETE_OLD_AWS_KEY_TASK_TYPE.equals(task.getType()) ||
            task.getStringArg() == null) {
          return false;
        }
        try {
          DeleteKeyTaskArg taskArgObject = DeleteKeyTaskArg.fromTask(task);

          ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.systemDefault());
          ZonedDateTime keyDeletionTime = ZonedDateTime.parse(taskArgObject.keyDeletionTime);
          return currentDate.isAfter(keyDeletionTime);

        } catch (KeyRotationException e) {
          Loggers.CLOUD.warn("Task to delete the old key is rejected: " + e.getMessage());
          task.finished();
          return false;
        }
      }

      @Override
      public void accept(final MultiNodeTasks.PerformingTask task) {
        DeleteKeyTaskArg taskArgObject = null;
        try {
          taskArgObject = DeleteKeyTaskArg.fromTask(task);
          Loggers.CLOUD.debug("Deleting the AWS key after rotation: " + ParamUtil.maskKey(taskArgObject.oldAccessKeyId));

          SProject curProject = myProjectManager.findProjectByExternalId(taskArgObject.projectId);
          if (curProject == null) {
            throw new KeyRotationException("The project with id " + taskArgObject.projectId + " does not exist");
          }
          OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(curProject, taskArgObject.connectionId);
          if (awsConnectionDescriptor == null) {
            throw new KeyRotationException("The connection with id " + taskArgObject.connectionId + " does not exist");
          }

          String connectionRegion = awsConnectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM);
          String currentAccessKeyId = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
          String secretAccessKey = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM);

          if(connectionRegion == null) {
            throw new KeyRotationException("The connection region cannot be null");
          }
          if(currentAccessKeyId == null) {
            throw new KeyRotationException("The connection with id " + taskArgObject.connectionId + " does not have access key id");
          }
          if(secretAccessKey == null) {
            throw new KeyRotationException("The connection with key " + ParamUtil.maskKey(currentAccessKeyId) + " does not have secret access key");
          }

          AmazonIdentityManagement iam = iamClientBuilder.createIamClient(
            connectionRegion,
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(currentAccessKeyId, secretAccessKey))
          );

          deletePreviousAccessKey(taskArgObject.oldAccessKeyId, iam);
          task.finished();

        } catch (KeyRotationException e) {
          String errMsg;
          if (taskArgObject == null) {
            errMsg = "Task to delete the old key cannot be completed: " + e.getMessage();
          } else {
            errMsg = "Cannot delete the old AWS key " + ParamUtil.maskKey(taskArgObject.oldAccessKeyId) + ": ";
          }

          Loggers.CLOUD.warnAndDebugDetails(errMsg, e);
          throw new CannotAcceptTaskException(errMsg + e.getMessage());
        }
      }
    });
  }

  private static void deletePreviousAccessKey(@NotNull final String awsAccessKeyId,
                                              @NotNull final AmazonIdentityManagement iam)
    throws KeyRotationException {
    DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
      .withAccessKeyId(awsAccessKeyId);
    try {
      iam.deleteAccessKey(deleteAccessKeyRequest);
    } catch (NoSuchEntityException | LimitExceededException | ServiceFailureException e) {
      throw new KeyRotationException(e);
    }
  }

  public void scheduleAwsKeyForDeletion(@NotNull final String awsAccessKeyId, @NotNull final String awsConnectionId, @NotNull final String externalProjectId) {

    ZonedDateTime keyDeletionTime = ZonedDateTime
      .now(ZoneId.systemDefault())
      .plus(getOldKeyPreserveTime());

    DeleteKeyTaskArg taskArgsObject = new DeleteKeyTaskArg(
      awsAccessKeyId,
      awsConnectionId,
      keyDeletionTime.toString(),
      externalProjectId
    );
    Pair<String, String> taskIdAndArg = taskArgsObject.toTaskArgs();

    MultiNodeTasks.TaskData task = new MultiNodeTasks.TaskData(
      DELETE_OLD_AWS_KEY_TASK_TYPE,
      taskIdAndArg.first,
      null, null,
      taskIdAndArg.second
    );
    myMultiNodeTasks.submit(task);

    Loggers.CLOUD.debug("Submitted task to delete the old AWS key with id: " + awsAccessKeyId);
  }

  @Used("tests")
  @NotNull
  public TemporalAmount getOldKeyPreserveTime() {
    return OLD_KEY_PRESERVE_TIME;
  }

  private static class DeleteKeyTaskArg {
    public final String oldAccessKeyId;
    public final String connectionId;
    public final String projectId;
    public final String keyDeletionTime;

    private DeleteKeyTaskArg(@NotNull final String oldAccessKeyId,
                             @NotNull final String connectionId,
                             @NotNull final String keyDeletionTime,
                             @NotNull final String projectId) {
      this.oldAccessKeyId = oldAccessKeyId;
      this.connectionId = connectionId;
      this.keyDeletionTime = keyDeletionTime;
      this.projectId = projectId;
    }

    public static DeleteKeyTaskArg fromTask(@NotNull final MultiNodeTasks.PerformingTask task) throws KeyRotationException {
      String taskIdentity = task.getIdentity();
      Pair<String, String> keyIdConnectionId = splitArgByTaskDivider(taskIdentity);
      Loggers.CLOUD.debug("Extracted args from delete old key task: old key access id is " + keyIdConnectionId.first + " and connection id is " + keyIdConnectionId.second);

      String taskStringArg = task.getStringArg();
      if (taskStringArg == null) {
        throw new KeyRotationException("Delete old key Task argument cannot be null");
      }
      Pair<String, String> keyDeletionTimeProjectId = splitArgByTaskDivider(taskStringArg);
      Loggers.CLOUD.debug("Extracted args from delete old key task: deletion date is " + keyDeletionTimeProjectId.first + " and project id is " + keyDeletionTimeProjectId.second);

      return new DeleteKeyTaskArg(
        keyIdConnectionId.first,
        keyIdConnectionId.second,
        keyDeletionTimeProjectId.first,
        keyDeletionTimeProjectId.second
      );
    }

    private static Pair<String, String> splitArgByTaskDivider(@NotNull final String arg) throws KeyRotationException {
      int identityDividerIndex = arg.indexOf(TASK_ARG_DIVIDER);
      if (identityDividerIndex < 0) {
        throw new KeyRotationException("Malformed task arg: " + arg);
      }

      String first = arg.substring(0, identityDividerIndex);
      String second = arg.substring(identityDividerIndex + TASK_ARG_DIVIDER.length());
      return new Pair<>(first, second);
    }

    public Pair<String, String> toTaskArgs() {
      StringBuilder mergedIdsBuilder = new StringBuilder();
      mergedIdsBuilder.append(oldAccessKeyId); // the acces key ID will never have <TASK_ARG_DIVIDER> characters inside it
      mergedIdsBuilder.append(TASK_ARG_DIVIDER);
      mergedIdsBuilder.append(connectionId); // is user-configurable and can potentially have <TASK_ARG_DIVIDER> characters inside it

      StringBuilder mergedArgsBuilder = new StringBuilder();
      mergedArgsBuilder.append(keyDeletionTime); // the ZonedDateTime will never have <TASK_ARG_DIVIDER> characters inside it
      mergedArgsBuilder.append(TASK_ARG_DIVIDER);
      mergedArgsBuilder.append(projectId); // is user-configurable and can potentially have <TASK_ARG_DIVIDER> characters inside it

      return new Pair<>(
        mergedIdsBuilder.toString(),
        mergedArgsBuilder.toString()
      );
    }
  }
}
