package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_GLOBAL_ENDPOINT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;

public class StsClientBuilder {
  public static final String ENFORCE_ALLOWLIST_AT_STS_CLIENT_PROPERTY_NAME = "teamcity.internal.aws.connection.stsEndpointAllowlistClientEnforcing.enabled";
  public static final String VALIDATE_NETWORK_TARGET_PROPERTY_NAME = "teamcity.internal.aws.connection.stsEndpointsValidateNetworkTarget.enabled";

  public static void addConfiguration(@NotNull AWSSecurityTokenServiceClientBuilder stsBuilder, @NotNull final Map<String, String> properties) {
    String stsEndpoint = properties.get(STS_ENDPOINT_PARAM);
    AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
      STS_GLOBAL_ENDPOINT,
      Regions.US_EAST_1.getName()
    );

    if (stsEndpoint != null && !stsEndpoint.equals(STS_GLOBAL_ENDPOINT)) {
      if (isSafeStsEndpoint(stsEndpoint)) {
        try {
          Regions awsRegion = Regions.fromName(properties.get(REGION_NAME_PARAM));
          endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
            stsEndpoint,
            awsRegion.getName()
          );

        } catch (IllegalArgumentException e) {
          Loggers.CLOUD.warn("Using the global STS endpoint - the region " + properties.get(REGION_NAME_PARAM) + " is invalid: " + e.getMessage());
        }
      } else {
        Loggers.CLOUD.warn("Rejected a non-allowlisted STS endpoint override, falling back to global STS: " + stsEndpoint);
      }
    }

    stsBuilder.withEndpointConfiguration(endpointConfiguration);
    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
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
