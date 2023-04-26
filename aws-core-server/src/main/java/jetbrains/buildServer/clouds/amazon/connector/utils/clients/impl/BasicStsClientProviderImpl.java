package jetbrains.buildServer.clouds.amazon.connector.utils.clients.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicStsClientProviderImpl implements StsClientProvider {

  @NotNull
  @Override
  public AWSSecurityTokenService getClientWithCredentials(@NotNull AwsConnectionCredentials awsConnectionCredentials, @Nullable Map<String, String> parameters)
    throws ConnectionCredentialsException {

    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withCredentials(awsConnectionCredentials.toAWSCredentialsProvider());
    if (parameters != null) {
      StsClientBuilder.addConfiguration(stsBuilder, parameters);
    }
    return stsBuilder.build();
  }

  @NotNull
  @Override
  public AWSSecurityTokenService getClient(@Nullable Map<String, String> parameters) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder
      .standard();
    if (parameters != null) {
      StsClientBuilder.addConfiguration(stsBuilder, parameters);
    }
    return stsBuilder.build();
  }
}
