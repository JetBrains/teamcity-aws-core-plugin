package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.serverSide.impl.executors.MockExecutorServices;
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
    myExecutorServices = new MockExecutorServices();
  }

  @Test
  public void givenAwsConnBuilder_whenWithStaticCredsFactory_thenReturnConnectorWithTwoKeys() {

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, testAccessKey);
    myConnectorProperties.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, testSecretKey);

    AWSCredentialsProvider awsCredentialsProvider = new BrokenCredentialsProvider();
    try {
      AwsCredentialsBuilder credentialsBuilder = myAwsConnectorFactory.getAwsCredentialsBuilderOfType(myConnectorProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM));
      awsCredentialsProvider = credentialsBuilder.createCredentialsProvider(myConnectorProperties);
    } catch (AwsConnectorException e) {
      fail("Connector builder threw exception: " + e.getMessage());
    }

    assertEquals("Access key should be equal", testAccessKey, awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
    assertEquals("Secret key should be equal", testSecretKey, awsCredentialsProvider.getCredentials().getAWSSecretKey());
  }

  @Test(expectedExceptions = {NoSuchAwsCredentialsBuilderException.class})
  public void givenAwsConnBuilder_whenWithUnknownCredsFactory_thenThrowException() throws AwsConnectorException {
    myAwsConnectorFactory.getAwsCredentialsBuilderOfType("UNKNOWN");
  }

  @Test(expectedExceptions = {IllegalStateException.class})
  public void givenAwsConnBuilderWithRegisteredFactory_whenTryingToRegisterTheSameType_thenThrowException() {

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);
    StaticCredentialsBuilder secondStaticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

  }

  @Test
  public void givenAwsConnBuilderAndStaticCredsFactiry_whenWithBadAccessKey_thenThrowExceptionWithParamName() {

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, "");
    myConnectorProperties.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, testSecretKey);

    try {
      AwsCredentialsBuilder credentialsBuilder = myAwsConnectorFactory.getAwsCredentialsBuilderOfType(myConnectorProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM));
      credentialsBuilder.createCredentialsProvider(myConnectorProperties);
    } catch (AwsConnectorException e) {
      assertEquals("Access key param should be in exception", AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, e.getParameterName());
    }
  }

  @Test
  public void givenAwsConnBuilderAndStaticCredsFactiry_whenWithBadSecretKey_thenThrowExceptionWithParamName() {

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myConnectorProperties.put(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    myConnectorProperties.put(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, testAccessKey);
    myConnectorProperties.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, "");

    try {
      AwsCredentialsBuilder credentialsBuilder = myAwsConnectorFactory.getAwsCredentialsBuilderOfType(myConnectorProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM));
      credentialsBuilder.createCredentialsProvider(myConnectorProperties);
    } catch (AwsConnectorException e) {
      assertEquals("Secret key param should be in exception", AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, e.getParameterName());
    }
  }
}