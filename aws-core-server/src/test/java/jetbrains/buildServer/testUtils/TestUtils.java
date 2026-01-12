package jetbrains.buildServer.testUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionDescriptorImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsCredentialsHolderCache;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static StsClientProvider getStsClientProvider(String testAccessKeyId, String testSecretAccessKey, String testSessionToken) throws ConnectionCredentialsException {
    StsClient securityTokenService = Mockito.mock(StsClient.class);
    Instant expirationInstant = Instant.now().plus(1, ChronoUnit.HOURS);
    when(securityTokenService.getSessionToken(any(GetSessionTokenRequest.class)))
      .thenReturn(GetSessionTokenResponse.builder().credentials(
        Credentials.builder()
          .accessKeyId(testAccessKeyId)
          .secretAccessKey(testSecretAccessKey)
          .sessionToken(testSessionToken)
          .expiration(expirationInstant)
          .build())
        .build());
    when(securityTokenService.assumeRole(any(AssumeRoleRequest.class)))
      .thenReturn(AssumeRoleResponse.builder()
        .credentials(
          Credentials.builder()
            .accessKeyId(testAccessKeyId)
            .secretAccessKey(testSecretAccessKey)
            .sessionToken(testSessionToken)
            .expiration(expirationInstant)
            .build()
        )
        .build());

    StsClientProvider stsClientProvider = Mockito.mock(StsClientProvider.class);
    when(stsClientProvider.getClientWithCredentials(any(), any()))
      .thenReturn(securityTokenService);

    return stsClientProvider;
  }

  public static StsClientProvider getStsClientProviderWithNoKeys() throws ConnectionCredentialsException {
    StsClient securityTokenService = Mockito.mock(StsClient.class);
    when(securityTokenService.getSessionToken(any(GetSessionTokenRequest.class)))
      .thenReturn(GetSessionTokenResponse.builder()
        .credentials(
          Credentials.builder()
            .accessKeyId("")
            .secretAccessKey("")
            .sessionToken("")
            .expiration(Instant.now())
            .build())
        .build());

    StsClientProvider stsClientProvider = Mockito.mock(StsClientProvider.class);
    when(stsClientProvider.getClientWithCredentials(any(), any()))
      .thenReturn(securityTokenService);

    return stsClientProvider;
  }

  public static AwsCredentialsHolderCache getAwsCredentialsHolderCache() {
    return new AwsCredentialsHolderCache(EventDispatcher.create(BuildServerListener.class), Mockito.mock(ProjectManager.class));
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
