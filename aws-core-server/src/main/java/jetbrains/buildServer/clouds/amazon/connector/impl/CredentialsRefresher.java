package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class CredentialsRefresher {

  protected static final int SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES = 1;
  protected static final int SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES = 2;

  private ScheduledFuture<?> myRefreshingTask;

  public CredentialsRefresher(@NotNull final AwsCredentialsHolder credentialsHolder,
                              @NotNull final ScheduledExecutorService scheduledExecutorService) {
    Date expirationDate = credentialsHolder.getSessionExpirationDate();
    if (expirationDate != null) {
      myRefreshingTask = scheduledExecutorService.scheduleWithFixedDelay(() -> {
        if (currentSessionExpired(expirationDate)) {
          Loggers.CLOUD.debug("Current Session of the temporary credentials has expired, refreshing...");
          credentialsHolder.refreshCredentials();
        }
      }, SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES, SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES, TimeUnit.MINUTES);
    }
  }

  public void stop() {
    myRefreshingTask.cancel(true);
  }

  @Used("Tests")
  public CredentialsRefresher() {
  }

  private boolean currentSessionExpired(@NotNull final Date expirationDate) {
    return Date.from(Instant.now().plusSeconds((SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES + SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES) * 60L))
               .after(expirationDate);
  }
}
