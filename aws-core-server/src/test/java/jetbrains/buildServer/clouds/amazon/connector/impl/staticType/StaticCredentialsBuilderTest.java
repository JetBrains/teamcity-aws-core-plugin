package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
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

  @Test
  public void givenAwsConnFactory_whenWithUnlnownCredentialsType_thenReturnThrowException() {
    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN");
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
    assertEquals("There should be one invalid property", 1, invalidProperties.size());
    assertEquals("The ivalid property reason sould be as described", "The credentials type UNKNOWN is not supported.", invalidProperties.get(0).getInvalidReason());
  }

  @Test
  public void givenAwsConnFactory_whenWithWithOnlyCredentialsTypeProp_thenReturnAllInvalidProps() {
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);

    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
    assertEquals("There should be three invalid properties (access, secret keys and region)", 3, invalidProperties.size());
  }

  @Test
  public void givenAwsConnFactory_whenWithAccessKeyIdOnly_thenReturnOneInvalidProp() {
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, testAccessKey);

    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
    assertEquals("There should be two invalid properties (Secret Access Key and region name)", 2, invalidProperties.size());
  }
}