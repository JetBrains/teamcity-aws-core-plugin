package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_DEFAULT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;

public class StsClientBuilder {
  public static void addConfiguration(@NotNull AWSSecurityTokenServiceClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);
    if (stsEndpoint != null && !stsEndpoint.equals(STS_ENDPOINT_DEFAULT)) {

      AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
        stsEndpoint,
        Regions.US_EAST_1.getName()
      );

      stsBuilder.withEndpointConfiguration(endpointConfiguration);
    }

    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }
}
