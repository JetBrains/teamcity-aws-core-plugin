package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions.AWSRegions;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;

public class EC2ClientCreator {

  @NotNull
  public Ec2Client createClient(@NotNull AwsConnectionBean connection) throws ConnectionCredentialsException {
    final AwsCredentialsData credentialsData = connection.getAwsCredentialsHolder().getAwsCredentials();
    final Ec2ClientBuilder builder = Ec2Client.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("ec2Client_" + connection.getConnectionId()))
      .overrideConfiguration(
        ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
          .build());

    final String accessKeyId = credentialsData.getAccessKeyId();
    final String secretAccessKey = credentialsData.getSecretAccessKey();
    final String sessionToken = credentialsData.getSessionToken();
    AwsCredentials credentials;
    if (sessionToken != null) {
      credentials = AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
    } else {
      credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    }

    builder.credentialsProvider(StaticCredentialsProvider.create(credentials));

    builder.region(AWSRegions.getRegion(connection.getRegion()));

    return builder.build();
  }
}

