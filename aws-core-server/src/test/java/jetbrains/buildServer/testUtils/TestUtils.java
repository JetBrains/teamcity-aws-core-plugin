package jetbrains.buildServer.testUtils;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static StsClientProvider getStsClientProvider(String testAccessKeyId, String testSecretAccessKey, String testSessionToken) throws AwsConnectorException {
    AWSSecurityTokenService securityTokenService = Mockito.mock(AWSSecurityTokenService.class);
    when(securityTokenService.getSessionToken(any()))
      .thenReturn(new GetSessionTokenResult().withCredentials(
        new Credentials(testAccessKeyId, testSecretAccessKey, testSessionToken, new Date()))
      );
    when(securityTokenService.assumeRole(any()))
      .thenReturn(new AssumeRoleResult().withCredentials(
        new Credentials(testAccessKeyId, testSecretAccessKey, testSessionToken, new Date()))
      );

    StsClientProvider stsClientProvider = Mockito.mock(StsClientProvider.class);
    when(stsClientProvider.getClientWithCredentials(any(), any()))
      .thenReturn(securityTokenService);

    return stsClientProvider;
  }

  public static StsClientProvider getStsClientProviderWithNoKeys() throws AwsConnectorException {
    AWSSecurityTokenService securityTokenService = Mockito.mock(AWSSecurityTokenService.class);
    when(securityTokenService.getSessionToken(any()))
      .thenReturn(new GetSessionTokenResult().withCredentials(
        new Credentials("", "", "", new Date()))
      );

    StsClientProvider stsClientProvider = Mockito.mock(StsClientProvider.class);
    when(stsClientProvider.getClientWithCredentials(any(), any()))
      .thenReturn(securityTokenService);

    return stsClientProvider;
  }
}
