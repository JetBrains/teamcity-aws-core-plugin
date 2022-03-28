package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public GetCallerIdentityResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AmazonClientException {
    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest()
      .withRequestCredentialsProvider(
        myAwsConnectorFactory.buildAwsCredentialsProvider(connectionProperties)
      );

    AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    StsClientBuilder.addConfiguration(stsClientBuilder, connectionProperties);
    AWSSecurityTokenService sts = stsClientBuilder.build();

    return sts.getCallerIdentity(getCallerIdentityRequest);
  }

  @Override
  @NotNull
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
    return myAwsConnectorFactory.validateProperties(connectionProperties);
  }
}
