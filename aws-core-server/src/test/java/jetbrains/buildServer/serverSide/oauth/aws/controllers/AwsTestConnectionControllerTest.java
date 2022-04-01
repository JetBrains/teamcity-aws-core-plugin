package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsTestConnectionControllerTest extends BaseControllerTestCase<AwsTestConnectionController> {

  private final String accessKey = "EXAMPLEACCESS";
  private final String secretKey = "EXAMPLESECRET";

  private final String testAccountId = "TEST ACCOUNT";
  private final String testArn = "TEST ARN";
  private final String testUserId = "TEST USER";

  private AwsConnectorFactory myAwsConnectorFactory;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    ExecutorServices myExecutorServices = new ExecutorServices() {
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
    AwsCredentialsBuilder registeredCredentialsBuilder = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);

    myProject = myFixture.createProject("AWS Connection Testing Project");
  }

  @Override
  protected AwsTestConnectionController createController() {
    return new AwsTestConnectionController(myServer, myWebManager, createTester());
  }

  private AwsConnectionTester createTester() {
    return new AwsConnectionTester() {
      @NotNull
      @Override
      public GetCallerIdentityResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AmazonClientException {
        return new GetCallerIdentityResult()
          .withAccount(testAccountId)
          .withArn(testArn)
          .withUserId(testUserId);
      }

      @NotNull
      @Override
      public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
        return myAwsConnectorFactory.getInvalidProperties(connectionProperties);
      }
    };
  }

  @Test
  public void givenAwsConnectionProperties_whentTesting_thenReturnCallerIdentity() throws Exception {
    String propertyPrefix = "prop:";
    doPost(propertyPrefix + CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE,
           propertyPrefix + AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, accessKey,
           propertyPrefix + AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, secretKey
    );

    List<Element> testResults = getAwsConnectionTestResultFromXmlResponse(myResponse.getReturnedContentAsXml());

    assertEquals(1, testResults.size());
    assertEquals(testAccountId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID));
    assertEquals(testUserId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ID));
    assertEquals(testArn, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ARN));
  }

  private List<Element> getAwsConnectionTestResultFromXmlResponse(Element xmlContent) {
    ArrayList<Element> elements = new ArrayList<>();

    List<Element> xmlCollection = xmlContent.getChildren(AWS_CALLER_IDENTITY_ELEMENT);
    for (Element element : xmlCollection) {
      elements.add(element);
    }

    return elements;
  }
}
