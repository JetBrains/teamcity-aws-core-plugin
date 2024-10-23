package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StaticSessionCredentialsHolder implements AwsCredentialsHolder {
  private final SProjectFeatureDescriptor myAwsConnectionFeature;
  private final AwsCredentialsHolder myBasicCredentialsHolder;
  private final StsClientProvider myStsClientProvider;

  public StaticSessionCredentialsHolder(@NotNull final SProjectFeatureDescriptor awsConnectionFeature,
                                        @NotNull final AwsCredentialsHolder basicCredentialsHolder,
                                        @NotNull final StsClientProvider stsClientProvider) {
    myAwsConnectionFeature = awsConnectionFeature;
    myBasicCredentialsHolder = basicCredentialsHolder;
    myStsClientProvider = stsClientProvider;
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() throws ConnectionCredentialsException {
    Credentials credentials = requestSession().getCredentials();
    return AwsConnectionUtils.getDataFromCredentials(credentials);
  }

  @Override
  public void refreshCredentials() {
    //TODO: TW-78235 refactor other parts of AWS Core plugin not to use refreshing logic
  }

  @Override
  @Nullable
  public Date getSessionExpirationDate() {
    //TODO: TW-78235 refactor other parts of AWS Core plugin not to use refreshing logic
    return null;
  }

  private GetSessionTokenResult requestSession() throws ConnectionCredentialsException {
    GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
    Map<String, String> connectionProperties = myAwsConnectionFeature.getParameters();

    AWSSecurityTokenService sts = myStsClientProvider
      .getClientWithCredentials(
        new AwsConnectionCredentials(
          myBasicCredentialsHolder.getAwsCredentials(),
          connectionProperties
        ),
        connectionProperties
      );

    int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
    getSessionTokenRequest.withDurationSeconds(sessionDurationMinutes * 60);

    return IOGuard.allowNetworkCall(() -> sts.getSessionToken(getSessionTokenRequest));
  }
}
