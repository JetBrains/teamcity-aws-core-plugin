package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.RotateKeyApi;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.CannotAcceptTaskException;
import jetbrains.buildServer.serverSide.MultiNodeTasks;
import jetbrains.buildServer.serverSide.ServerResponsibility;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ConcurrentHashMap;

public class OldKeysCleaner {

  private static final String DELETE_OLD_AWS_KEY_TASK_TYPE = "deleteOldAwsKey";

  private final MultiNodeTasks myMultiNodeTasks;
  private final ServerResponsibility myServerResponsibility;
  private final TemporalAmount myOldKeyPreserveTime;

  private final ConcurrentHashMap<String, RotateKeyApi> oldKeysRotateApis = new ConcurrentHashMap<>();

  public OldKeysCleaner(@NotNull MultiNodeTasks multiNodeTasks,
                        @NotNull final ServerResponsibility serverResponsibility,
                        @NotNull final TemporalAmount oldKeyPreserveTime) {
    myMultiNodeTasks = multiNodeTasks;
    myServerResponsibility = serverResponsibility;
    myOldKeyPreserveTime = oldKeyPreserveTime;

    myMultiNodeTasks.subscribe(DELETE_OLD_AWS_KEY_TASK_TYPE, new MultiNodeTasks.TaskConsumer() {
      @Override
      public boolean beforeAccept(@NotNull final MultiNodeTasks.PerformingTask task) {
        if (! myServerResponsibility.canWriteToConfigDirectory() ||
            ! DELETE_OLD_AWS_KEY_TASK_TYPE.equals(task.getType()) ||
            task.getStringArg() == null) {
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
          RotateKeyApi previousKeyRotateApi = oldKeysRotateApis.get(previousAccessKeyId);
          if (previousKeyRotateApi == null) {
            throw new KeyRotationException("The task to delete the key could not find it in the scheduled for deletion keys map");
          }
          previousKeyRotateApi.deletePreviousAccessKey();
          task.finished();

        } catch (KeyRotationException e) {
          String errMsg = "Cannot delete the old AWS key " + ParamUtil.maskKey(previousAccessKeyId) + ": ";
          Loggers.CLOUD.warnAndDebugDetails(errMsg, e);
          throw new CannotAcceptTaskException(errMsg + e.getMessage());

        } finally {
          oldKeysRotateApis.remove(previousAccessKeyId);
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
