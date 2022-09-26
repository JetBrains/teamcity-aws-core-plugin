package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public class StaticSessionCredentialsHolder implements AwsCredentialsHolder {

  private final AWSSecurityTokenService mySts;
  private final GetSessionTokenRequest mySessionConfiguration;
  private volatile GetSessionTokenResult currentSession;

  public StaticSessionCredentialsHolder(@NotNull final AwsCredentialsHolder credentialsHolder,
                                        @NotNull final Map<String, String> connectionProperties) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withCredentials(AwsConnectionUtils.awsCredsProviderFromHolder(credentialsHolder));
    StsClientBuilder.addConfiguration(stsBuilder, connectionProperties);
    mySts = stsBuilder.build();

    int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
    mySessionConfiguration = new GetSessionTokenRequest()
      .withDurationSeconds(sessionDurationMinutes * 60);

    currentSession = mySts.getSessionToken(mySessionConfiguration);
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    Credentials credentials = currentSession.getCredentials();
    return AwsConnectionUtils.getDataFromCredentials(credentials);
  }

  @Override
  public void refreshCredentials() {
    Loggers.CLOUD.debug("Refreshing AWS Credentials...");
    try {
      currentSession = mySts.getSessionToken(mySessionConfiguration);
    } catch (Exception e) {
      Loggers.CLOUD.warnAndDebugDetails("Failed to refresh AWS Credentials: ", e);
    }
  }

  @Override
  @NotNull
  public Date getSessionExpirationDate() {
    return currentSession.getCredentials().getExpiration();
  }
}
