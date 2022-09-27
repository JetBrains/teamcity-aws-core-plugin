package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import com.intellij.openapi.diagnostic.Logger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;

public class AwsCredentialsRefresheringManager {
  protected static final int SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES = 1;
  protected static final int SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES = 2;
  private static final Logger LOG = Logger.getInstance(AwsCredentialsRefresheringManager.class.getName());
  private final ScheduledExecutorService myRefresherExecutor;
  private final ConcurrentHashMap<String, AwsConnectionDescriptor> myAwsConnectionsWithAutoRefresh = new ConcurrentHashMap<>();

  public AwsCredentialsRefresheringManager() {
    myRefresherExecutor = ExecutorsFactory.newFixedScheduledDaemonExecutor("AWS Credentials Refresher executor", 1);
    myRefresherExecutor
      .scheduleWithFixedDelay(
        new CredentialsRefresherTask(),
        SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES,
        SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES,
        TimeUnit.MINUTES
      );
  }

  public void scheduleCredentialRefreshingTask(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    String awsConnectionId = awsConnectionDescriptor.getId();
    myAwsConnectionsWithAutoRefresh.put(awsConnectionId, awsConnectionDescriptor);
    LOG.debug("Added credentials to auto-refresh collection for AWS Connection with ID: " + awsConnectionId);
  }

  public void stopCredentialsRefreshingtask(@NotNull final String awsConnectionId) {
    myAwsConnectionsWithAutoRefresh.remove(awsConnectionId);
    LOG.debug("Stopped credentials auto-refresh for AWS Connection with ID: " + awsConnectionId);
  }

  public void dispose() {
    myAwsConnectionsWithAutoRefresh.clear();
    myRefresherExecutor.shutdown();
  }

  private class CredentialsRefresherTask implements Runnable {

    @Override
    public void run() {
      myAwsConnectionsWithAutoRefresh.forEach((awsConnectionId, awsConnectionDescriptor) -> {
        Date expirationDate = awsConnectionDescriptor.getAwsCredentialsHolder().getSessionExpirationDate();
        if (expirationDate != null && currentSessionExpired(expirationDate)) {
          Loggers.CLOUD.debug("Refreshing Session Credentials for AWS Connection with ID: " + awsConnectionId);
          awsConnectionDescriptor.getAwsCredentialsHolder().refreshCredentials();
        }
      });
    }

    private boolean currentSessionExpired(@NotNull final Date expirationDate) {
      return Date.from(Instant.now().plus(SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES + SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES, ChronoUnit.MINUTES))
                 .after(expirationDate);
    }
  }
}
