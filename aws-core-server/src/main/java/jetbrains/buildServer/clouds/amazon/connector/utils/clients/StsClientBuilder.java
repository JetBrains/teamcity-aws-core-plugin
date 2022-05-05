package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import org.jetbrains.annotations.NotNull;

public class StsClientBuilder {
  public static void addConfiguration(@NotNull final AWSSecurityTokenServiceClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    String stsEndpoint = properties.get(AwsAccessKeysParams.STS_ENDPOINT_PARAM);
    if (stsEndpoint != null && ! stsEndpoint.equals(AwsAccessKeysParams.STS_ENDPOINT_DEFAULT)) {

      AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
        stsEndpoint,
        Regions.US_EAST_1.getName()
      );

      stsBuilder.withEndpointConfiguration(endpointConfiguration);
    }

    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }
}
