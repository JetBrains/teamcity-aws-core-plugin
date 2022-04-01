package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StaticCredentialsBuilderTest extends BaseTestCase {
  private final String testAccessKey = "TESTACCESS";

  private AwsConnectorFactory myAwsConnectorFactory;
  private Map<String, String> myConnectorProperties;

  private ExecutorServices myExecutorServices;

  @BeforeMethod
  public void setup() {
    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    myConnectorProperties = new HashMap<>();
    myExecutorServices = new ExecutorServices() {
      @NotNull
      @Override
      public ScheduledExecutorService getNormalExecutorService() {
        return Executors.newScheduledThreadPool(1);
      }

      @NotNull
      @Override
      public ExecutorService getLowPriorityExecutorService() {
        return Executors.newSingleThreadExecutor();
      }
    };
  }

  @Test(expectedExceptions = {NoSuchAwsCredentialsBuilderException.class})
  public void givenAwsConnFactory_whenWithUnlnownCredentialsType_thenReturnThrowException() throws NoSuchAwsCredentialsBuilderException {
    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN");
    myAwsConnectorFactory.validateProperties(myConnectorProperties);
  }

  @Test
  public void givenAwsConnFactory_whenWithWithOnlyCredentialsTypeProp_thenReturnAllInvalidProps(){
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);

    try {
      List<InvalidProperty> invalidProperties = myAwsConnectorFactory.validateProperties(myConnectorProperties);
      assertEquals("There should be two invalid properties", 2, invalidProperties.size());
    } catch (NoSuchAwsCredentialsBuilderException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void givenAwsConnFactory_whenWithAccessKeyIdOnly_thenReturnOneInvalidProp(){
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, testAccessKey);

    try {
      List<InvalidProperty> invalidProperties = myAwsConnectorFactory.validateProperties(myConnectorProperties);
      assertEquals("There should be one invalid property (Secret Access Key)", 1, invalidProperties.size());
      assertEquals("The ivalid property name sould be the secret access key", AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, invalidProperties.get(0).getPropertyName());
      assertEquals("The ivalid property reason sould be as described", "Please provide the secret access key ", invalidProperties.get(0).getInvalidReason());
    } catch (NoSuchAwsCredentialsBuilderException e) {
      fail(e.getMessage());
    }
  }
}