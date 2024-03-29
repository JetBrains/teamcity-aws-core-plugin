package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public AwsTestConnectionResult testConnection(@NotNull final ProjectFeatureDescriptorImpl connectionFeature) throws ConnectionCredentialsException {
    AwsCredentialsHolder testCredentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionFeature);

    AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    StsClientBuilder.addConfiguration(stsClientBuilder, connectionFeature.getParameters());
    AWSSecurityTokenService sts = stsClientBuilder.build();

    return IOGuard.allowNetworkCall(() ->
                                      new AwsTestConnectionResult(
                                        sts.getCallerIdentity(createGetCallerIdentityRequest(testCredentialsHolder, connectionFeature))
                                      )
    );
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

    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest();
    getCallerIdentityRequest.withRequestCredentialsProvider(
      awsConnectionCredentials.toAWSCredentialsProvider()
    );
    return getCallerIdentityRequest;
  }
}
