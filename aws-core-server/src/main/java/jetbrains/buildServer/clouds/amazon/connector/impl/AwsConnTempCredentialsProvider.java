package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;

public class AwsConnTempCredentialsProvider implements AWSCredentialsProvider {

  private final AWSSecurityTokenService mySts;
  private final GetSessionTokenRequest mySessionConfiguration;
  private final int sessionCredentialsValidThresholdMinutes = 10;

  private GetSessionTokenResult currentSession;


  public AwsConnTempCredentialsProvider(AWSSecurityTokenService sts, int sessionDurationMinutes, ExecutorServices executorServices) {
    executorServices.getNormalExecutorService().scheduleAtFixedRate(getRefreshTask(), 1, sessionCredentialsValidThresholdMinutes, TimeUnit.MINUTES);

    mySts = sts;

    mySessionConfiguration = new GetSessionTokenRequest()
      .withDurationSeconds(sessionDurationMinutes * 60);
    currentSession = sts.getSessionToken(mySessionConfiguration);
  }

  @Override
  public AWSCredentials getCredentials() {
    return new BasicSessionCredentials(
      currentSession.getCredentials().getAccessKeyId(),
      currentSession.getCredentials().getSecretAccessKey(),
      currentSession.getCredentials().getSessionToken()
    );
  }

  @Override
  public void refresh() {
    currentSession = mySts.getSessionToken(mySessionConfiguration);
  }

  private Runnable getRefreshTask() {
    return new Runnable() {
      @Override
      public void run() {
        if (Date.from(Instant.now().plusSeconds(sessionCredentialsValidThresholdMinutes * 60L)).after(currentSession.getCredentials().getExpiration())) {
          refresh();
        }
      }
    };
  }
}
