package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;

public abstract class CredentialsRefresher implements AWSCredentialsProvider {

  protected static final int SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES = 1;
  protected static final int SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES = 2;
  protected AWSSecurityTokenService mySts;

  public CredentialsRefresher(@NotNull final AWSCredentialsProvider awsCredentialsProvider,
                              @NotNull final Map<String, String> connectionProperties,
                              @NotNull final ExecutorServices executorServices) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withCredentials(awsCredentialsProvider);
    StsClientBuilder.addConfiguration(stsBuilder, connectionProperties);
    mySts = stsBuilder.build();

    executorServices.getNormalExecutorService().scheduleWithFixedDelay(() -> {
      if (currentSessionExpired(getSessionExpirationDate())) {
        Loggers.CLOUD.debug("Current Session of the temporary credentials has expired, refreshing...");
        refresh();
      }
    }, SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES, SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES, TimeUnit.MINUTES);
  }

  @NotNull
  public abstract Date getSessionExpirationDate();

  private boolean currentSessionExpired(@NotNull final Date expirationDate) {
    return Date.from(Instant.now().plusSeconds((SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES + SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES) * 60L))
               .after(expirationDate);
  }
}
