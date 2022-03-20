package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;

public class AwsConnTempCredentialsProvider implements AWSCredentialsProvider {

  private final AWSSecurityTokenService mySts;
  private final GetSessionTokenRequest mySessionConfiguration;
  private final int sessionCredentialsValidThresholdMinutes = 1;
  private final int sessionCredentialsValidHandicapMinutes = 2;

  private volatile GetSessionTokenResult currentSession;


  public AwsConnTempCredentialsProvider(@NotNull final AWSSecurityTokenService sts,
                                        final int sessionDurationMinutes,
                                        @NotNull final ExecutorServices executorServices) {
    mySessionConfiguration = new GetSessionTokenRequest()
      .withDurationSeconds(sessionDurationMinutes * 60);

    currentSession = sts.getSessionToken(mySessionConfiguration);

    executorServices.getNormalExecutorService().scheduleWithFixedDelay(() -> {
      if(currentSessionExpired())
        refresh();
    }, sessionCredentialsValidHandicapMinutes, sessionCredentialsValidThresholdMinutes, TimeUnit.MINUTES);

    mySts = sts;
  }

  @Override
  @NotNull
  public AWSCredentials getCredentials() {
    Credentials credentials = currentSession.getCredentials();
    return new BasicSessionCredentials(
      credentials.getAccessKeyId(),
      credentials.getSecretAccessKey(),
      credentials.getSessionToken()
    );
  }

  @Override
  public void refresh() {
    try {
      currentSession = mySts.getSessionToken(mySessionConfiguration);
    } catch (Exception e){
      Loggers.CLOUD.debug("Failed to refresh AWS Credentials: " + e.getMessage());
    }
  }

  private boolean currentSessionExpired(){
    return Date.from(Instant.now().plusSeconds((sessionCredentialsValidThresholdMinutes + sessionCredentialsValidHandicapMinutes) * 60L))
               .after(currentSession.getCredentials().getExpiration());
  }
}
