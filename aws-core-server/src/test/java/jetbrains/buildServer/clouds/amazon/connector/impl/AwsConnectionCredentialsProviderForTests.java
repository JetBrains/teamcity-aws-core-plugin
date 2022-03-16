package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;

public class AwsConnectionCredentialsProviderForTests implements AWSCredentialsProvider {

  private final long mySessionCredentialsValidThresholdMilis;
  private final long mySessionDurationMilis;

  private GetSessionTokenResult currentSession;


  public AwsConnectionCredentialsProviderForTests(ExecutorServices executorServices, long sessionCredentialsValidThresholdMilis, long sessionDurationMilis) {
    mySessionCredentialsValidThresholdMilis = sessionCredentialsValidThresholdMilis;
    mySessionDurationMilis = sessionDurationMilis;
    executorServices.getNormalExecutorService().scheduleAtFixedRate(getRefreshTask(), 100, sessionCredentialsValidThresholdMilis, TimeUnit.MILLISECONDS);

    currentSession = new GetSessionTokenResult()
      .withCredentials(new Credentials()
                         .withAccessKeyId(UUID.randomUUID().toString())
                         .withSecretAccessKey(UUID.randomUUID().toString())
                         .withSessionToken(UUID.randomUUID().toString())
                         .withExpiration(Date.from(Instant.now().plusMillis(sessionDurationMilis)))
      );
    Loggers.CLOUD.warn("Created the creds provider");
  }

  @Override
  public AWSCredentials getCredentials() {
    Loggers.CLOUD.warn("Return credentials");
    return new BasicSessionCredentials(
      currentSession.getCredentials().getAccessKeyId(),
      currentSession.getCredentials().getSecretAccessKey(),
      currentSession.getCredentials().getSessionToken()
    );
  }

  @Override
  public void refresh() {
    Loggers.CLOUD.warn("Refresh credentials");
    currentSession = new GetSessionTokenResult()
      .withCredentials(new Credentials()
                         .withAccessKeyId(UUID.randomUUID().toString())
                         .withSecretAccessKey(UUID.randomUUID().toString())
                         .withSessionToken(UUID.randomUUID().toString())
                         .withExpiration(Date.from(Instant.now().plusMillis(mySessionDurationMilis)))
      );
  }

  private Runnable getRefreshTask() {
    return new Runnable() {
      @Override
      public void run() {
        Loggers.CLOUD.warn("Checking the credentials for expiraition");
        if (Date.from(Instant.now().plusMillis(mySessionCredentialsValidThresholdMilis)).after(currentSession.getCredentials().getExpiration())) {
          Loggers.CLOUD.warn("Credentials have expired");
          refresh();
        }
      }
    };
  }
}
