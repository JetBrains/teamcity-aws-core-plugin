package jetbrains.buildServer.testUtils;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import java.util.Date;
import java.util.Map;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionDescriptorImpl;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static StsClientProvider getStsClientProvider(String testAccessKeyId, String testSecretAccessKey, String testSessionToken) throws ConnectionCredentialsException {
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

  public static StsClientProvider getStsClientProviderWithNoKeys() throws ConnectionCredentialsException {
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

  public static ConnectionDescriptor createConnectionDescriptor(String projectId, String connectionId, Map<String, String> params) {
    return new AwsConnectionDescriptorImpl(
      new SProjectFeatureDescriptor() {
        @NotNull
        @Override
        public String getProjectId() {
          return projectId;
        }

        @NotNull
        @Override
        public String getId() {
          return connectionId;
        }

        @NotNull
        @Override
        public String getType() {
          return OAuthConstants.FEATURE_TYPE;
        }

        @NotNull
        @Override
        public Map<String, String> getParameters() {
          return params;
        }
      },
      Mockito.mock(AwsCredentialsHolder.class),
      Mockito.mock(ExtensionHolder.class)
    );
  }
}
