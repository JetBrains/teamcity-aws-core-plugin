//package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;
//
//import jetbrains.buildServer.BaseTestCase;
//import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
//import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
//import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
//import jetbrains.buildServer.clouds.amazon.connector.errors.NoSuchAwsCredentialsBuilderException;
//import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
//import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
//import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
//import jetbrains.buildServer.serverSide.InvalidProperty;
//import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
//import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
//import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
//import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
//import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;
//
//public class StaticCredentialsBuilderTest extends BaseTestCase {
//
//  private final String testAccessKeyId = "TEST_ACCESS";
//  private final String testSecretAccessKey = "TEST_SECRET";
//  private AwsConnectorFactory myAwsConnectorFactory;
//  private Map<String, String> myConnectorProperties;
//
//  @BeforeMethod
//  public void setup() throws Exception {
//    super.setUp();
//    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
//    myConnectorProperties = createDefaultProperties();
//  }
//
//  @Test
//  public void givenAwsConnFactory_whenWithAllProperties_thenReturnAwsCredentialsProvider() {
//    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
//
//    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
//    assertTrue(invalidProperties.isEmpty());
//
//    try {
//      AwsCredentialsHolder credentialsHolder = staticCredentialsFactory.constructSpecificCredentialsProvider(createProjectFeatureDescriptor(myConnectorProperties));
//      assertEquals(testAccessKeyId, credentialsHolder.getAwsCredentials().getAccessKeyId());
//      assertEquals(testSecretAccessKey, credentialsHolder.getAwsCredentials().getSecretAccessKey());
//    } catch (AwsConnectorException awsConnectorException) {
//      fail("Could not construct the credentials provider: " + awsConnectorException.getMessage());
//    }
//  }
//
//  @Test
//  public void givenAwsConnFactory_withoutKeys_thenReturnInvalidPropsWithKeysErrors() {
//    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
//
//    myConnectorProperties.remove(ACCESS_KEY_ID_PARAM);
//    myConnectorProperties.remove(SECURE_SECRET_ACCESS_KEY_PARAM);
//
//    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
//
//    assertTrue(invalidProperties.containsAll(Arrays.asList(
//      new InvalidProperty(
//        ACCESS_KEY_ID_PARAM,
//        ACCESS_KEY_ID_ERROR
//      ),
//      new InvalidProperty(
//        SECURE_SECRET_ACCESS_KEY_PARAM,
//        SECRET_ACCESS_KEY_ERROR
//      )
//    )));
//  }
//
//  @Test
//  public void givenAwsConnFactory_withoutRegionParam_thenReturnInvalidPropsWithRegionError() {
//    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
//
//    myConnectorProperties.remove(REGION_NAME_PARAM);
//    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
//
//    assertTrue(invalidProperties.contains(
//      new InvalidProperty(
//        REGION_NAME_PARAM,
//        REGION_ERROR
//      )
//    ));
//  }
//
//  @Test
//  public void givenAwsConnFactory_withInvalidSessionDuration_thenReturnInvalidPropsWithSessionDurationError() {
//    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
//
//    myConnectorProperties.put(SESSION_DURATION_PARAM, String.valueOf(MAX_SESSION_DURATION + 1));
//    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(myConnectorProperties);
//
//    assertTrue(invalidProperties.contains(
//      new InvalidProperty(
//        SESSION_DURATION_PARAM,
//        SESSION_DURATION_ERROR
//      )
//    ));
//  }
//
//  @Test ( expectedExceptions = { NoSuchAwsCredentialsBuilderException.class } )
//  public void givenAwsConnFactory_withoutCredentialsType_thenThrowException() throws AwsConnectorException {
//    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory);
//
//    myConnectorProperties.remove(CREDENTIALS_TYPE_PARAM);
//    AwsCredentialsHolder awsCredentials = myAwsConnectorFactory.buildAwsCredentialsProvider(createProjectFeatureDescriptor(myConnectorProperties));
//  }
//
//  private SProjectFeatureDescriptor createProjectFeatureDescriptor(Map<String, String> connectionProperties) {
//    return new ProjectFeatureDescriptorImpl(
//      "",
//      AwsConnectionProvider.TYPE,
//      connectionProperties,
//      ""
//    );
//  }
//
//  private Map<String, String> createDefaultProperties() {
//    Map<String, String> res = new HashMap<>();
//    res.put(ACCESS_KEY_ID_PARAM, testAccessKeyId);
//    res.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, testSecretAccessKey);
//    res.put(CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
//    res.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
//    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
//    res.put(SESSION_CREDENTIALS_PARAM, "false");
//    return res;
//  }
//}