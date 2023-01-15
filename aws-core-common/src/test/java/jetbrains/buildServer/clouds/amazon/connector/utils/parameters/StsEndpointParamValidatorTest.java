package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import jetbrains.buildServer.BaseTestCase;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_ALL_AWS_ENDPOINTS_EXPRESSION;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME;
import static org.testng.Assert.*;

public class StsEndpointParamValidatorTest extends BaseTestCase {
  private final String CHINA_ENDPOINT_URL_1= "https://sts.cn-north-1.amazonaws.com.cn";
  private final String CHINA_ENDPOINT_URL_2 = "https://sts.cn-northwest-1.amazonaws.com.cn";


  @Test
  public void when_china_endpoint_then_accept_without_errors() {
    removeInternalProperty(STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME);

    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_1));
    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_2));
  }

  @Test
  public void when_with_all_aws_endpoints_expr_plus_custom_then_accept_all_aws_endpoints_and_custom() {
    String customEndpoint = "http://some.custom.com";
    String internalPropValue = STS_ENDPOINTS_ALLOWLIST_ALL_AWS_ENDPOINTS_EXPRESSION + "," + customEndpoint;
    setInternalProperty(STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, internalPropValue);

    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_1));
    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_2));
    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(customEndpoint));
  }
}