package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import jetbrains.buildServer.serverSide.executors.ExecutorServices;

public class AwsConnectionIdSyncTask implements Runnable {

  private final ExecutorServices myExecutorServices;
  private final AwsConnectionIdSynchroniser myAwsConnectionIdSynchroniser;

  public AwsConnectionIdSyncTask(ExecutorServices executorServices,
                                 AwsConnectionIdSynchroniser awsConnectionIdSynchroniser) {
    myExecutorServices = executorServices;
    myAwsConnectionIdSynchroniser = awsConnectionIdSynchroniser;
  }

  @Override
  public void run() {
    myAwsConnectionIdSynchroniser.run();
    myExecutorServices.getLowPriorityExecutorService().submit(this);
  }
}
