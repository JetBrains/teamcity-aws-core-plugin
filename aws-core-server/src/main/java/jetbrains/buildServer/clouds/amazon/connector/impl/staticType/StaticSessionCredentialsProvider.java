package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.impl.CredentialsRefresher;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;

public class StaticSessionCredentialsProvider extends CredentialsRefresher {

  private final GetSessionTokenRequest mySessionConfiguration;

  private volatile GetSessionTokenResult currentSession;

  public StaticSessionCredentialsProvider(@NotNull final AWSCredentialsProvider awsCredentialsProvider,
                                          @NotNull final Map<String, String> connectionProperties,
                                          @NotNull final ExecutorServices executorServices) {
    super(awsCredentialsProvider, connectionProperties, executorServices);

    int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
    mySessionConfiguration = new GetSessionTokenRequest()
      .withDurationSeconds(sessionDurationMinutes * 60);

    currentSession = getSts().getSessionToken(mySessionConfiguration);
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
      currentSession = getSts().getSessionToken(mySessionConfiguration);
    } catch (Exception e) {
      Loggers.CLOUD.debug("Failed to refresh AWS Credentials: " + e.getMessage());
    }
  }

  public boolean currentSessionExpired() {
    return Date.from(Instant.now().plusSeconds((sessionCredentialsValidThresholdMinutes + sessionCredentialsValidHandicapMinutes) * 60L))
               .after(currentSession.getCredentials().getExpiration());
  }
}
