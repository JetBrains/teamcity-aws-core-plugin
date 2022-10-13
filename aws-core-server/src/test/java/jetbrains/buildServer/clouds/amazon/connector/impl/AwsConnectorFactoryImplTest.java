package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;

public class AwsConnectorFactoryImplTest extends BaseTestCase {

  private AwsConnectorFactory myAwsConnectorFactory;
  private Map<String, String> myConnectorProperties;

  @BeforeMethod
  public void setup() {
    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    myConnectorProperties = new HashMap<>();
  }

  @Test
  public void givenAwsConnFactory_withUnknownCredentialsType_thenReturnInvalidPropsWithCredsTypeError() {
    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN");
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "UNKNOWN" + " is not supported."
      )
    ));
  }

  @Test
  public void givenAwsConnFactory_withoutCredsTypeProp_thenReturnInvalidPropsWithCredsTypeError() {
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "null" + " is not supported."
      )
    ));
  }

  @Test(expectedExceptions = {IllegalStateException.class})
  public void givenAwsConnBuilderWithRegisteredFactory_whenTryingToRegisterTheSameType_thenThrowException() {
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
    StaticCredentialsBuilder secondStaticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
  }
}