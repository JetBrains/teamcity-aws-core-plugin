package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AwsConnectorFactoryImplTest extends BaseTestCase {

  private final String testAccessKey = "TESTACCESS";
  private final String testSecretKey = "TESTSECRET";
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
  public void givenAwsConnBuilder_whenWithStaticCredsFactory_thenReturnConnectorWithTwoKeys() throws AwsConnectorException {
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, testAccessKey);
    myConnectorProperties.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, testSecretKey);
    myConnectorProperties.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);

    AwsCredentialsHolder credentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(myConnectorProperties);

    assertEquals("Access key should be equal", testAccessKey, credentialsHolder.getAwsCredentials().getAccessKeyId());
    assertEquals("Secret key should be equal", testSecretKey, credentialsHolder.getAwsCredentials().getSecretAccessKey());
  }

  @Test ( expectedExceptions = {NoSuchAwsCredentialsBuilderException.class} )
  public void givenAwsConnBuilder_whenWithUnknownCredsFactory_thenThrowException() throws AwsConnectorException {
    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN");
    AwsCredentialsHolder credentialsHolder = myAwsConnectorFactory.buildAwsCredentialsProvider(myConnectorProperties);
    assertEquals("Access key should be empty", "", credentialsHolder.getAwsCredentials().getAccessKeyId());
    assertEquals("Secret key should be empty", "", credentialsHolder.getAwsCredentials().getSecretAccessKey());
  }

  @Test(expectedExceptions = {IllegalStateException.class})
  public void givenAwsConnBuilderWithRegisteredFactory_whenTryingToRegisterTheSameType_thenThrowException() {
    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);
    StaticCredentialsBuilder secondStaticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);
  }
}