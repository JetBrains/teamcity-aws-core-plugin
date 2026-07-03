package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.TeamCityStsClientBuilder;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public AwsTestConnectionResult testConnection(@NotNull final ProjectFeatureDescriptorImpl connectionFeature) throws ConnectionCredentialsException {
    AwsCredentialsHolder testCredentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionFeature);

    StsClientBuilder stsClientBuilder = StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD);
    TeamCityStsClientBuilder.addConfiguration(stsClientBuilder, connectionFeature.getParameters());
    try (StsClient sts = stsClientBuilder.build()) {
      return IOGuard.allowNetworkCall(() ->
        new AwsTestConnectionResult(
          sts.getCallerIdentity(createGetCallerIdentityRequest(testCredentialsHolder, connectionFeature))
        )
      );
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
    return myAwsConnectorFactory.getInvalidProperties(connectionProperties);
  }

  @NotNull
  private GetCallerIdentityRequest createGetCallerIdentityRequest(@NotNull final AwsCredentialsHolder awsCredentialsHolder,
                                                                  @NotNull final ProjectFeatureDescriptorImpl connectionFeature)
    throws ConnectionCredentialsException {
    AwsConnectionCredentials awsConnectionCredentials = new AwsConnectionCredentials(
      awsCredentialsHolder.getAwsCredentials(),
      connectionFeature.getParameters()
    );

    AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = AwsRequestOverrideConfiguration.builder()
      .credentialsProvider(awsConnectionCredentials.toAWSCredentialsProvider())
      .build();

    return GetCallerIdentityRequest.builder()
      .overrideConfiguration(awsRequestOverrideConfiguration)
      .build();
  }
}
