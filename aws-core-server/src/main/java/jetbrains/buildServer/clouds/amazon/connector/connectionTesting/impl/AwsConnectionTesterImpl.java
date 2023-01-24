package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.utils.AwsConnectionUtils;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.StsClientBuilder;
import jetbrains.buildServer.connections.aws.AwsConnectionCredentialsFactoryImpl;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.connections.credentials.errors.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionTesterImpl implements AwsConnectionTester {
  private final AwsConnectionCredentialsFactoryImpl myAwsConnectionCredentialsFactory;

  public AwsConnectionTesterImpl(@NotNull final AwsConnectionCredentialsFactoryImpl awsConnectionCredentialsFactory) {
    myAwsConnectionCredentialsFactory = awsConnectionCredentialsFactory;
  }

  @Override
  @NotNull
  public AwsTestConnectionResult testConnection(@NotNull final ProjectFeatureDescriptorImpl connectionFeature) throws ConnectionCredentialsException {
    return IOGuard.allowNetworkCall(
      () -> {
        AWSSecurityTokenServiceClientBuilder stsClientBuilder = AWSSecurityTokenServiceClientBuilder.standard();
        StsClientBuilder.addConfiguration(stsClientBuilder, connectionFeature.getParameters());
        AWSSecurityTokenService sts = stsClientBuilder.build();

        AwsConnectionDescriptor testConnectionDescriptor = myAwsConnectionCredentialsFactory.requestCredentials(connectionFeature);
        return new AwsTestConnectionResult(
          sts.getCallerIdentity(createGetCallerIdentityRequest(testConnectionDescriptor))
        );
      }
    );
  }

  @Override
  @NotNull
  public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
    return new ArrayList<>(myAwsConnectionCredentialsFactory.getPropertiesProcessor().process((connectionProperties)));
  }

  @NotNull
  private GetCallerIdentityRequest createGetCallerIdentityRequest(@NotNull final AwsConnectionDescriptor awsConnectionDescriptor) {
    GetCallerIdentityRequest getCallerIdentityRequest = new GetCallerIdentityRequest();
    getCallerIdentityRequest.withRequestCredentialsProvider(
      AwsConnectionUtils.awsCredsProviderFromData(awsConnectionDescriptor.getConnectionCredentials())
    );
    return getCallerIdentityRequest;
  }
}
