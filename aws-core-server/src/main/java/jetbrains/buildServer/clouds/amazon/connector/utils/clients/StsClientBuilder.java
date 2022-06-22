package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_DEFAULT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;

public class StsClientBuilder {
  public static void addConfiguration(@NotNull AWSSecurityTokenServiceClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);
    if (stsEndpoint == null || stsEndpoint.equals(STS_ENDPOINT_DEFAULT)) {

      String region = properties.get(REGION_NAME_PARAM);
      try {
        stsBuilder.withRegion(Regions.fromName(region));
      } catch (IllegalArgumentException e) {
        Loggers.CLOUD.warn("Using the global STS endpoint - the region " + region + " is invalid: " + e.getMessage());
      }

    } else {
      String someRegionForCustomEndpoint = Regions.US_EAST_1.getName();
      Loggers.CLOUD.debug("Setting custom STS endpoint: " + stsEndpoint + ". Will use " + someRegionForCustomEndpoint + " region for the EndpointConfiguration");

      AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
        stsEndpoint,
        someRegionForCustomEndpoint
      );

      stsBuilder.withEndpointConfiguration(endpointConfiguration);
    }

    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }
}
