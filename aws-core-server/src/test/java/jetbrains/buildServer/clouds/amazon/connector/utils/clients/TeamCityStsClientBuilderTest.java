package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_ENDPOINT_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.STS_GLOBAL_ENDPOINT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.REGION_NAME_PARAM;
import static org.testng.Assert.assertEquals;

public class TeamCityStsClientBuilderTest extends BaseTestCase {

  private static final String ALLOWLISTED_REGION_ENDPOINT = "https://192.0.2.1/";

  @Test
  public void givenAllowlistedEndpoint_thenEndpointIsOverridden() {
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, ALLOWLISTED_REGION_ENDPOINT);

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, ALLOWLISTED_REGION_ENDPOINT);
    properties.put(REGION_NAME_PARAM, "eu-central-1");

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(ALLOWLISTED_REGION_ENDPOINT));
    assertEquals(captureRegion(stsBuilder), Region.of("eu-central-1"));
  }

  @Test
  public void givenNoEndpointConfigured_thenFallsBackToGlobalEndpoint() {
    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, new HashMap<>());

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
    assertEquals(captureRegion(stsBuilder), Region.US_EAST_1);
  }

  @Test
  public void givenGlobalEndpointExplicitlyConfiguredWithDifferentRegion_thenRegionIsNotOverridden() {
    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, STS_GLOBAL_ENDPOINT);
    properties.put(REGION_NAME_PARAM, "eu-central-1");

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
    assertEquals(captureRegion(stsBuilder), Region.US_EAST_1);
  }

  @Test
  public void givenNonAllowlistedEndpoint_thenFallsBackToGlobalEndpoint() {
    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, "http://attacker.example/");

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
  }

  @Test
  public void givenAllowlistedButMetadataAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String metadataEndpoint = "http://169.254.169.254/latest/meta-data/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, metadataEndpoint);

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, metadataEndpoint);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
  }

  @Test
  public void givenAllowlistedButLoopbackAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String loopbackEndpoint = "http://127.0.0.1/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, loopbackEndpoint);

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, loopbackEndpoint);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
  }

  @Test
  public void givenAllowlistedButAnyLocalAddressEndpoint_thenFallsBackToGlobalEndpoint() {
    String anyLocalEndpoint = "http://0.0.0.0/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, anyLocalEndpoint);

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, anyLocalEndpoint);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
  }

  @Test
  public void givenAllowlistedButUnresolvableEndpoint_thenFallsBackToGlobalEndpoint() {
    String unresolvableEndpoint = "https://sts.unresolvable.invalid/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, unresolvableEndpoint);

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, unresolvableEndpoint);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(STS_GLOBAL_ENDPOINT));
  }

  @Test
  public void givenAllowlistedButMetadataAddressEndpoint_whenNetworkTargetCheckDisabled_thenEndpointIsHonored() {
    String metadataEndpoint = "http://169.254.169.254/latest/meta-data/";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, metadataEndpoint);
    setInternalProperty(TeamCityStsClientBuilder.VALIDATE_NETWORK_TARGET_PROPERTY_NAME, "false");

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, metadataEndpoint);

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create(metadataEndpoint));
  }

  @Test
  public void givenNonAllowlistedEndpoint_whenAllowlistEnforcementDisabled_thenEndpointIsHonored() {
    setInternalProperty(TeamCityStsClientBuilder.ENFORCE_ALLOWLIST_AT_STS_CLIENT_PROPERTY_NAME, "false");

    StsClientBuilder stsBuilder = Mockito.mock(StsClientBuilder.class, Mockito.RETURNS_SELF);
    Map<String, String> properties = new HashMap<>();
    properties.put(STS_ENDPOINT_PARAM, "http://attacker.example/");
    properties.put(REGION_NAME_PARAM, "us-east-1");

    TeamCityStsClientBuilder.addConfiguration(stsBuilder, properties);

    assertEquals(captureEndpoint(stsBuilder), URI.create("http://attacker.example/"));
  }

  @NotNull
  private URI captureEndpoint(StsClientBuilder stsBuilder) {
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    Mockito.verify(stsBuilder).endpointOverride(uriCaptor.capture());
    return uriCaptor.getValue();
  }

  @NotNull
  private Region captureRegion(StsClientBuilder stsBuilder) {
    ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
    Mockito.verify(stsBuilder).region(regionCaptor.capture());
    return regionCaptor.getValue();
  }
}
