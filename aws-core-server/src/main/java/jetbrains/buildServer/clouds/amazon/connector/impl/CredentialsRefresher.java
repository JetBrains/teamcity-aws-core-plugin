package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;

public abstract class CredentialsRefresher implements AWSCredentialsProvider {

  protected final int sessionCredentialsValidThresholdMinutes = 1;
  protected final int sessionCredentialsValidHandicapMinutes = 2;
  private AWSSecurityTokenService mySts;

  public CredentialsRefresher(@NotNull final AWSCredentialsProvider awsCredentialsProvider,
                              @NotNull final Map<String, String> connectionProperties,
                              @NotNull final ExecutorServices executorServices) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withCredentials(awsCredentialsProvider);
    StsClientBuilder.addConfiguration(stsBuilder, connectionProperties);
    mySts = stsBuilder.build();

    executorServices.getNormalExecutorService().scheduleWithFixedDelay(() -> {
      if (currentSessionExpired()) {
        refresh();
      }
    }, sessionCredentialsValidHandicapMinutes, sessionCredentialsValidThresholdMinutes, TimeUnit.MINUTES);
  }

  protected AWSSecurityTokenService getSts() {
    return mySts;
  }

  protected void setSts(AWSSecurityTokenService sts) {
    mySts = sts;
  }

  public abstract boolean currentSessionExpired();
}
