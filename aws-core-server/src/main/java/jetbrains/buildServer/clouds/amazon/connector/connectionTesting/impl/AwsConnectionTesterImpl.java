package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
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
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectorFactory myAwsConnectorFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectorFactory = awsConnectorFactory;
  }

  @Override
  @NotNull
  public AwsTestConnectionResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AmazonClientException {

    try {
      AwsCredentialsHolder credentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(connectionProperties);

      GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest();
      if(credentialsHolder.getAwsCredentials().getSessionToken() == null) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(
          credentialsHolder.getAwsCredentials().getAccessKeyId(),
          credentialsHolder.getAwsCredentials().getSecretAccessKey()
        );
        getCallerIdentityRequest.withRequestCredentialsProvider(
          new AWSStaticCredentialsProvider(basicAWSCredentials)
        );
      } else {
        BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
          credentialsHolder.getAwsCredentials().getAccessKeyId(),
          credentialsHolder.getAwsCredentials().getSecretAccessKey(),
          credentialsHolder.getAwsCredentials().getSessionToken()
        );
        getCallerIdentityRequest.withRequestCredentialsProvider(
          new AWSStaticCredentialsProvider(basicSessionCredentials)
        );
      }

      AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
      StsClientBuilder.addConfiguration(stsClientBuilder, connectionProperties);
      AWSSecurityTokenService sts = stsClientBuilder.build();

      return new AwsTestConnectionResult(sts.getCallerIdentity(getCallerIdentityRequest));
    } catch (AwsConnectorException e) {
      throw new AmazonClientException("Could not create the AWSCredentialsProvider: " + e.getMessage());
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
    return myAwsConnectorFactory.getInvalidProperties(connectionProperties);
  }
}
