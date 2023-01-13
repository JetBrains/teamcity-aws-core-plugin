package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StsEndpointParamValidatorTest {
  private final String CHINA_ENDPOINT_URL_1= "https://sts.cn-north-1.amazonaws.com.cn";
  private final String CHINA_ENDPOINT_URL_2 = "https://sts.cn-northwest-1.amazonaws.com.cn";


  @Test
  public void when_china_endpoint_then_accept_without_errors() {
    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_1));
    assertTrue(StsEndpointParamValidator.isValidStsEndpoint(CHINA_ENDPOINT_URL_2));
  }
}