package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;

public class StsClientBuilder {
  public static void addConfiguration(@NotNull AWSSecurityTokenServiceClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);
    AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
      STS_GLOBAL_ENDPOINT,
      Regions.US_EAST_1.getName()
    );

    if (! stsEndpoint.equals(STS_GLOBAL_ENDPOINT)) {
      try {
        Regions awsRegion = Regions.fromName(properties.get(REGION_NAME_PARAM));
        endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
          stsEndpoint,
          awsRegion.getName()
        );

      } catch (IllegalArgumentException e) {
        Loggers.CLOUD.warn("Using the global STS endpoint - the region " + properties.get(REGION_NAME_PARAM) + " is invalid: " + e.getMessage());
      }
    }

    stsBuilder.withEndpointConfiguration(endpointConfiguration);
    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }
}
