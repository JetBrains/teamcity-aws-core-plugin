package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.RotateKeyApi;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.CannotAcceptTaskException;
import jetbrains.buildServer.serverSide.MultiNodeTasks;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ConcurrentHashMap;

public class OldKeysCleaner {

  private static final String DELETE_OLD_AWS_KEY_TASK_TYPE = "deleteOldAwsKey";

  private final MultiNodeTasks myMultiNodeTasks;
  private final TemporalAmount myOldKeyPreserveTime;

  private final ConcurrentHashMap<String, RotateKeyApi> oldKeysRotateApis = new ConcurrentHashMap<>();

  public OldKeysCleaner(@NotNull MultiNodeTasks multiNodeTasks,
                        @NotNull final TemporalAmount oldKeyPreserveTime) {
    myMultiNodeTasks = multiNodeTasks;
    myOldKeyPreserveTime = oldKeyPreserveTime;

    myMultiNodeTasks.subscribe(DELETE_OLD_AWS_KEY_TASK_TYPE, new MultiNodeTasks.TaskConsumer() {
      @Override
      public boolean beforeAccept(@NotNull final MultiNodeTasks.PerformingTask task) {
        if (!DELETE_OLD_AWS_KEY_TASK_TYPE.equals(task.getType()) || task.getStringArg() == null) {
          return false;
        }

        ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime keyDeletionTime = ZonedDateTime.parse(task.getStringArg());
        return currentDate.isAfter(keyDeletionTime);
      }

      @Override
      public void accept(final MultiNodeTasks.PerformingTask task) {
        String previousAccessKeyId = task.getIdentity();
        Loggers.CLOUD.debug("Deleting the AWS key after rotation: " + ParamUtil.maskKey(previousAccessKeyId));
        try {
          oldKeysRotateApis.get(previousAccessKeyId).deletePreviousAccessKey();
        } catch (KeyRotationException e) {
          String errMsg = "Cannot delete the old AWS key " + ParamUtil.maskKey(previousAccessKeyId) + ": ";
          Loggers.CLOUD.warnAndDebugDetails(errMsg, e);
          throw new CannotAcceptTaskException(errMsg + e.getMessage());

        } finally {
          oldKeysRotateApis.remove(previousAccessKeyId);
          task.finished();
        }
      }
    });
  }

  public void scheduleAwsKeyForDeletion(@NotNull final String awsAccessKeyId, @NotNull final RotateKeyApi awsRotateApi) {
    oldKeysRotateApis.put(awsAccessKeyId, awsRotateApi);

    ZonedDateTime keyDeletionTime = ZonedDateTime
      .now(ZoneId.systemDefault())
      .plus(myOldKeyPreserveTime);

    MultiNodeTasks.TaskData task = new MultiNodeTasks.TaskData(
      DELETE_OLD_AWS_KEY_TASK_TYPE,
      awsAccessKeyId,
      null, null,
      keyDeletionTime.toString()
    );
    myMultiNodeTasks.submit(task);

    Loggers.CLOUD.debug("Submitted task to delete the old AWS key with id: " + awsAccessKeyId);
  }
}
