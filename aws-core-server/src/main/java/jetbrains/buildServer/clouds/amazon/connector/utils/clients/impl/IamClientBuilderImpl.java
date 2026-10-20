

package jetbrains.buildServer.clouds.amazon.connector.utils.clients.impl;

import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.IamClientBuilder;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;

public class IamClientBuilderImpl implements IamClientBuilder {
  @NotNull
  @Override
  public IamClient createIamClient(@NotNull String connectionRegion, @NotNull AwsCredentialsProvider credentials) {
    return IamClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .region(Region.of(connectionRegion))
      .credentialsProvider(credentials)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("iam"))
      .overrideConfiguration(ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
        .build()
      )
      .build();
  }
}