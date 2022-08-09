package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

public class EC2ClientCreator {

  @NotNull
  public AmazonEC2 createClient(@NotNull AwsConnectionBean connection) {
    final AwsCredentialsData credentialsData = connection.getAwsCredentialsHolder().getAwsCredentials();
    final AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard()
                                                                 .withClientConfiguration(AWSCommonParams.createClientConfigurationEx("ec2Client_" + connection.getConnectionId()));

    final String accessKeyId = credentialsData.getAccessKeyId();
    final String secretAccessKey = credentialsData.getSecretAccessKey();
    final String sessionToken = credentialsData.getSessionToken();
    AWSCredentials credentials;
    if (sessionToken != null) {
      credentials = new BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken);
    } else {
      credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
    }
    builder.withCredentials(new AWSStaticCredentialsProvider(credentials));

    builder.withRegion(connection.getRegion());

    return builder.build();
  }
}

