package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.testUtils.AbstractAwsConnectionTest;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.STS_ENDPOINT_DEFAULT;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.MAX_SESSION_DURATION;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;
import static jetbrains.buildServer.testUtils.TestUtils.getAwsCredentialsHolderCache;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProvider;

public class StaticSessionCredentialsHolderTest extends AbstractAwsConnectionTest {
  private final String TEST_ACCESS_KEY_ID = "TEST_ACCESS";
  private final String TEST_SECRET_ACCESS_KEY = "TEST_SECRET";


  private final String TEST_SESSION_ACCESS_KEY_ID = "TEST_ACCESS";
  private final String TEST_SESSION_SECRET_ACCESS_KEY = "TEST_SECRET";
  private final String TEST_SESSION_TOKEN = "TEST_TOKEN";

  private SProject myProject;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myProject = getMockedProject(testProjectId, createDefaultStorageValues());

    addTeamCityProject(myProject);

    addTeamCityAwsConnection(myProject, new ProjectFeatureDescriptorImpl(
      testConnectionId,
      AwsConnectionProvider.TYPE,
      myAwsDefaultConnectionProperties,
      myProject.getProjectId()
    ));

    new StaticCredentialsBuilder(
      getAwsConnectorFactory(),
      Mockito.mock(AwsConnectionCredentialsFactory.class),
      getStsClientProvider(TEST_SESSION_ACCESS_KEY_ID, TEST_SESSION_SECRET_ACCESS_KEY, TEST_SESSION_TOKEN),
      getAwsCredentialsHolderCache()
    );
  }

  @Test
  public void givenAwsConnFactory_whenWithAllProperties_thenReturnSessionAwsCredentials() {
    try {
      SProjectFeatureDescriptor connectionFeature = myProject.findFeatureById(testConnectionId);
      assert connectionFeature != null;
      AwsCredentialsHolder credentialsHolder = getAwsConnectorFactory()
        .buildAwsCredentialsProvider(connectionFeature);
      assertEquals(TEST_SESSION_ACCESS_KEY_ID, credentialsHolder.getAwsCredentials().getAccessKeyId());
      assertEquals(TEST_SESSION_SECRET_ACCESS_KEY, credentialsHolder.getAwsCredentials().getSecretAccessKey());
      assertEquals(TEST_SESSION_TOKEN, credentialsHolder.getAwsCredentials().getSessionToken());

    } catch (ConnectionCredentialsException awsConnectorException) {
      fail("Could not construct the credentials provider: " + awsConnectorException.getMessage());
    }
  }

  @Test
  public void givenAwsConnFactory_withInvalidSessionDuration_thenReturnInvalidPropsWithSessionDurationError() {
    Map<String, String> connProps = createConnectionDefaultProperties();
    connProps.put(SESSION_DURATION_PARAM, String.valueOf(MAX_SESSION_DURATION + 1));
    List<InvalidProperty> invalidProperties = getAwsConnectorFactory().getInvalidProperties(connProps);

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        SESSION_DURATION_PARAM,
        SESSION_DURATION_ERROR
      )
    ));
  }

  @Override
  public Map<String, String> createConnectionDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(ACCESS_KEY_ID_PARAM, TEST_ACCESS_KEY_ID);
    res.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, TEST_SECRET_ACCESS_KEY);
    res.put(CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    res.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
    res.put(SESSION_CREDENTIALS_PARAM, "true");
    return res;
  }
}