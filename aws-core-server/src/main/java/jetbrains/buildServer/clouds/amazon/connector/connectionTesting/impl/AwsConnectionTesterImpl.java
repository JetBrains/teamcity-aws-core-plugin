package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.credentials.AwsCredsHolderToProvider;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public AwsTestConnectionResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AwsConnectorException {
    AwsCredentialsHolder credentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionProperties);

    AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    StsClientBuilder.addConfiguration(stsClientBuilder, connectionProperties);
    AWSSecurityTokenService sts = stsClientBuilder.build();

    return new AwsTestConnectionResult(
      sts.getCallerIdentity(createGetCallerIdentityRequest(credentialsHolder))
    );
  }

  @Override
  @NotNull
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
    return myAwsConnectorFactory.getInvalidProperties(connectionProperties);
  }

  @NotNull
  private GetCallerIdentityRequest createGetCallerIdentityRequest(@NotNull final AwsCredentialsHolder awsCredentialsHolder) {
    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest();
    getCallerIdentityRequest.withRequestCredentialsProvider(
      AwsCredsHolderToProvider.convert(awsCredentialsHolder)
    );
    return getCallerIdentityRequest;
  }
}
