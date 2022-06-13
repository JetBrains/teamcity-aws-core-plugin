package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class CredentialsRefresher implements AwsCredentialsHolder {

  protected static final int SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES = 1;
  protected static final int SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES = 2;
  protected AWSSecurityTokenService mySts;

  public CredentialsRefresher(@NotNull final AwsCredentialsHolder credentialsHolder,
                              @NotNull final Map<String, String> connectionProperties,
                              @NotNull final ExecutorServices executorServices) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withCredentials(AwsConnectionUtils.awsCredsProviderFromHolder(credentialsHolder));
    StsClientBuilder.addConfiguration(stsBuilder, connectionProperties);
    mySts = stsBuilder.build();

    executorServices.getNormalExecutorService().scheduleWithFixedDelay(() -> {
      if (currentSessionExpired(getSessionExpirationDate())) {
        Loggers.CLOUD.debug("Current Session of the temporary credentials has expired, refreshing...");
        refreshCredentials();
      }
    }, SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES, SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES, TimeUnit.MINUTES);
  }

  @Used("Tests")
  public CredentialsRefresher(){}

  @NotNull
  public abstract Date getSessionExpirationDate();

  @NotNull
  protected AwsCredentialsData getDataFromCredentials(Credentials credentials){
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getSecretAccessKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return credentials.getSessionToken();
      }
    };
  }

  private boolean currentSessionExpired(@NotNull final Date expirationDate) {
    return Date.from(Instant.now().plusSeconds((SESSION_CREDENTIALS_VALID_THRESHOLD_MINUTES + SESSION_CREDENTIALS_VALID_HANDICAP_MINUTES) * 60L))
               .after(expirationDate);
  }
}
