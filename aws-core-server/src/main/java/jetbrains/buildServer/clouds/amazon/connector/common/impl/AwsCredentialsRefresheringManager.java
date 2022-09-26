package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import com.intellij.openapi.diagnostic.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.impl.CredentialsRefresher;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;

public class AwsCredentialsRefresheringManager {
  private static final Logger LOG = Logger.getInstance(AwsCredentialsRefresheringManager.class.getName());

  private final ScheduledExecutorService myQueue;
  private final ConcurrentHashMap<String, CredentialsRefresher> myAwsConnectionRefreshers = new ConcurrentHashMap<>();

  public AwsCredentialsRefresheringManager() {
    myQueue = ExecutorsFactory.newFixedScheduledDaemonExecutor("AWS Credentials Refresher executor", 1);
  }

  public void scheduleCredentialRefreshingTask(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    String awsConnectionId = awsConnectionDescriptor.getId();
    CredentialsRefresher credentialsRefresher = new CredentialsRefresher(awsConnectionDescriptor.getAwsCredentialsHolder(), myQueue);
    if (myAwsConnectionRefreshers.containsKey(awsConnectionId)) {
      stopCredentialsRefreshingtask(awsConnectionId);
    }
    myAwsConnectionRefreshers.put(awsConnectionId, credentialsRefresher);
    LOG.debug("Added credentials refreshing task for AWS Connection with ID: " + awsConnectionId);
  }

  public void stopCredentialsRefreshingtask(@NotNull final String awsConnectionId) {
    CredentialsRefresher credentialsRefresher = myAwsConnectionRefreshers.get(awsConnectionId);
    if (credentialsRefresher != null) {
      LOG.debug("Stopped credentials refreshing task for AWS Connection with ID: " + awsConnectionId);
      credentialsRefresher.stop();
    } else {
      LOG.debug("There is no credentials refreshing task for AWS Connection with ID: " + awsConnectionId);
    }
  }

  public void dispose() {
    myAwsConnectionRefreshers.forEach((awsConnectionId, refresher) -> refresher.stop());
    myQueue.shutdown();
  }
}
