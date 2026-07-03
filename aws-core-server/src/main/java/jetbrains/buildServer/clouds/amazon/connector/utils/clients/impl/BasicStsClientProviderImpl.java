package jetbrains.buildServer.clouds.amazon.connector.utils.clients.impl;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientProvider;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.TeamCityStsClientBuilder;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

public class BasicStsClientProviderImpl implements StsClientProvider {

  @NotNull
  @Override
  public StsClient getClientWithCredentials(@NotNull AwsConnectionCredentials awsConnectionCredentials, @Nullable Map<String, String> parameters)
    throws ConnectionCredentialsException {

    StsClientBuilder stsBuilder = StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .credentialsProvider(awsConnectionCredentials.toAWSCredentialsProvider());
    if (parameters != null) {
      TeamCityStsClientBuilder.addConfiguration(stsBuilder, parameters);
    }
    return stsBuilder.build();
  }

  @NotNull
  @Override
  public StsClient getClient(@Nullable Map<String, String> parameters) {
    StsClientBuilder stsBuilder = StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD);
    if (parameters != null) {
      TeamCityStsClientBuilder.addConfiguration(stsBuilder, parameters);
    }
    return stsBuilder.build();
  }
}
