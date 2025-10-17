package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import java.net.URI;
import java.util.Map;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_GLOBAL_ENDPOINT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;

public class TeamCityStsClientBuilder {
  public static void addConfiguration(@NotNull StsClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    URI endpoint = URI.create(STS_GLOBAL_ENDPOINT);
    Region region = Region.US_EAST_1;

    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);

    if (StringUtil.isNotEmpty(stsEndpoint) && !stsEndpoint.equals(STS_GLOBAL_ENDPOINT)) {
      try {
        endpoint = URI.create(stsEndpoint);
        region = Region.of(properties.get(REGION_NAME_PARAM));
      } catch (IllegalArgumentException | NullPointerException e) {
        Loggers.CLOUD.warn("Falling back to the global STS parameters: " + e.getMessage());
      }
    }

    stsBuilder.endpointOverride(endpoint)
      .region(region)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("sts"))
      .overrideConfiguration(ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
        .build()
      );
  }
}
