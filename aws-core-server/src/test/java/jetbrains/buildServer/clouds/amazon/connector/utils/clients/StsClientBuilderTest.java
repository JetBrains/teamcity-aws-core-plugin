package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_GLOBAL_ENDPOINT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;
import static org.testng.Assert.assertEquals;

public class StsClientBuilderTest extends BaseTestCase {

  private static final String ALLOWLISTED_REGION_ENDPOINT = "https://192.0.2.1/";

  @Test
  public void givenAllowlistedEndpoint_thenEndpointIsOverridden() {
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, ALLOWLISTED_REGION_ENDPOINT);

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, ALLOWLISTED_REGION_ENDPOINT);
    properties.put(REGION_NAME_PARAM, "eu-central-1");

    AwsClientBuilder.EndpointConfiguration endpointConfiguration = configure(properties);

    assertEquals(endpointConfiguration.getServiceEndpoint(), ALLOWLISTED_REGION_ENDPOINT);
    assertEquals(endpointConfiguration.getSigningRegion(), Regions.EU_CENTRAL_1.getName());
  }

  @Test
  public void givenNoEndpointConfigured_thenFallsBackToGlobalEndpoint() {
    AwsClientBuilder.EndpointConfiguration endpointConfiguration = configure(new HashMap<>());

    assertEquals(endpointConfiguration.getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
    assertEquals(endpointConfiguration.getSigningRegion(), Regions.US_EAST_1.getName());
  }

  @Test
  public void givenGlobalEndpointExplicitlyConfiguredWithDifferentRegion_thenRegionIsNotOverridden() {
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, STS_GLOBAL_ENDPOINT);
    properties.put(REGION_NAME_PARAM, "eu-central-1");

    AwsClientBuilder.EndpointConfiguration endpointConfiguration = configure(properties);

    assertEquals(endpointConfiguration.getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
    assertEquals(endpointConfiguration.getSigningRegion(), Regions.US_EAST_1.getName());
  }

  @Test
  public void givenNonAllowlistedEndpoint_thenFallsBackToGlobalEndpoint() {
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, "http://attacker.example/");

    assertEquals(configure(properties).getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
  }

  @Test
  public void givenAllowlistedButMetadataAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String metadataEndpoint = "http://169.254.169.254/latest/meta-data/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, metadataEndpoint);

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, metadataEndpoint);

    assertEquals(configure(properties).getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
  }

  @Test
  public void givenAllowlistedButLoopbackAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String loopbackEndpoint = "http://127.0.0.1/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, loopbackEndpoint);

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, loopbackEndpoint);

    assertEquals(configure(properties).getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
  }

  @Test
  public void givenAllowlistedButAnyLocalAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String anyLocalEndpoint = "http://0.0.0.0/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, anyLocalEndpoint);

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, anyLocalEndpoint);

    assertEquals(configure(properties).getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
  }

  @Test
  public void givenAllowlistedButUnresolvableEndpoint_thenFallsBackToGlobalEndpoint() {
    String unresolvableEndpoint = "https://sts.unresolvable.invalid/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, unresolvableEndpoint);

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, unresolvableEndpoint);

    assertEquals(configure(properties).getServiceEndpoint(), STS_GLOBAL_ENDPOINT);
  }

  @Test
  public void givenAllowlistedButMetadataAddressEndpoint_whenNetworkTargetCheckDisabled_thenEndpointIsHonored() {
    String metadataEndpoint = "http://169.254.169.254/latest/meta-data/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, metadataEndpoint);
    setInternalProperty(StsClientBuilder.VALIDATE_NETWORK_TARGET_PROPERTY_NAME, "false");

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, metadataEndpoint);
    properties.put(REGION_NAME_PARAM, "us-east-1");

    assertEquals(configure(properties).getServiceEndpoint(), metadataEndpoint);
  }

  @Test
  public void givenNonAllowlistedEndpoint_whenAllowlistEnforcementDisabled_thenEndpointIsHonored() {
    setInternalProperty(StsClientBuilder.ENFORCE_ALLOWLIST_AT_STS_CLIENT_PROPERTY_NAME, "false");

    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, "http://attacker.example/");
    properties.put(REGION_NAME_PARAM, "us-east-1");

    assertEquals(configure(properties).getServiceEndpoint(), "http://attacker.example/");
  }

  private AwsClientBuilder.EndpointConfiguration configure(Map<String, String> properties) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    StsClientBuilder.addConfiguration(stsBuilder, properties);
    return stsBuilder.getEndpoint();
  }
}
