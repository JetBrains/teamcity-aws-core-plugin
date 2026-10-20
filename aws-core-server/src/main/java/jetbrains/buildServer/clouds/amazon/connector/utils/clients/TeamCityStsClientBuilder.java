package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_GLOBAL_ENDPOINT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;

public class TeamCityStsClientBuilder {
  public static final String ENFORCE_ALLOWLIST_AT_STS_CLIENT_PROPERTY_NAME = "teamcity.internal.aws.connection.stsEndpointAllowlistClientEnforcing.enabled";
  public static final String VALIDATE_NETWORK_TARGET_PROPERTY_NAME = "teamcity.internal.aws.connection.stsEndpointsValidateNetworkTarget.enabled";

  public static void addConfiguration(@NotNull StsClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    URI endpoint = URI.create(STS_GLOBAL_ENDPOINT);
    Region region = Region.US_EAST_1;

    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);

    if (StringUtil.isNotEmpty(stsEndpoint) && !stsEndpoint.equals(STS_GLOBAL_ENDPOINT)) {
      if (isSafeStsEndpoint(stsEndpoint)) {
        try {
          endpoint = URI.create(stsEndpoint);
          region = Region.of(properties.get(REGION_NAME_PARAM));
        } catch (IllegalArgumentException | NullPointerException e) {
          Loggers.CLOUD.warn("Falling back to the global STS parameters: " + e.getMessage());
        }
      } else {
        Loggers.CLOUD.warn("Rejected a non-allowlisted STS endpoint override, falling back to global STS: " + stsEndpoint);
      }
    }

    stsBuilder.endpointOverride(endpoint)
      .region(region)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder("sts"))
      .overrideConfiguration(ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
        .build()
      );
  }

  private static boolean isSafeStsEndpoint(@NotNull final String stsEndpoint) {
    if (!enforceAllowlist()) {
      return true;
    }
    return StsEndpointParamValidator.isValidStsEndpoint(stsEndpoint) && !resolvesToDisallowedNetworkTarget(stsEndpoint);
  }

  private static boolean resolvesToDisallowedNetworkTarget(@NotNull final String url) {
    if (!TeamCityProperties.getBooleanOrTrue(VALIDATE_NETWORK_TARGET_PROPERTY_NAME)) return false;

    try {
      final String host = URI.create(url).getHost();
      if (host == null) return true;

      InetAddress[] addresses = IOGuard.allowNetworkCall(() -> InetAddress.getAllByName(host));
      for (InetAddress address : addresses) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
          return true;
        }
      }

      return false;
    } catch (UnknownHostException ex) {
      return true;
    }
  }

  private static boolean enforceAllowlist() {
    return TeamCityProperties.getBooleanOrTrue(ENFORCE_ALLOWLIST_AT_STS_CLIENT_PROPERTY_NAME);
  }
}
