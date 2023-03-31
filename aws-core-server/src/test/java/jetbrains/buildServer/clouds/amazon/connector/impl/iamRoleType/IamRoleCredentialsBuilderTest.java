package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType.externalId.AwsExternalIdsManagerImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.testUtils.AbstractAwsConnectionTest;
import jetbrains.buildServer.clouds.amazon.connector.impl.LinkedAwsConnectionProviderImpl;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.aws.AwsCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProvider;
import static org.mockito.Mockito.when;

public class IamRoleCredentialsBuilderTest extends AbstractAwsConnectionTest {

  protected final String TEST_PRINCIPAL_AWS_CONN_ID = "PROJECT_FEATURE_ID_PRINCIPAL";
  protected final String TEST_IAM_ROLE_AWS_CONN_ID = "PROJECT_FEATURE_ID_IAM_ROLE";
  private final String TEST_ACCESS_KEY_ID = "TEST_ACCESS";
  private final String TEST_SECRET_ACCESS_KEY = "TEST_SECRET";
  private final String TEST_IAM_ROLE_SESSION_ACCESS_KEY_ID = "TEST_ACCESS";
  private final String TEST_IAM_ROLE_SESSION_SECRET_ACCESS_KEY = "TEST_SECRET";
  private final String TEST_IAM_ROLE_SESSION_TOKEN = "TEST_TOKEN";
  private final String TEST_IAM_ROLE_ARN_PARAM = "arn:partition:service:region:account:resource";
  private SProject myProject;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myProject = getMockedProject(testProjectId, createDefaultStorageValues());
    when(myProject.getExternalId())
      .thenReturn(testConnectionId + "_EXTERNAL");

    addTeamCityProject(myProject);

    addTeamCityAwsConnection(myProject, new ProjectFeatureDescriptorImpl(
      TEST_PRINCIPAL_AWS_CONN_ID,
      AwsConnectionProvider.TYPE,
      myAwsDefaultConnectionProperties,
      myProject.getProjectId()
    ));
    addTeamCityAwsConnection(myProject, new ProjectFeatureDescriptorImpl(
      TEST_IAM_ROLE_AWS_CONN_ID,
      AwsConnectionProvider.TYPE,
      createIamRoleConnectionProperties(),
      myProject.getProjectId()
    ));

    ProjectConnectionCredentialsManager projectConnectionCredentialsManager = Mockito.mock(ProjectConnectionCredentialsManager.class);
    when(projectConnectionCredentialsManager.requestConnectionCredentials(myProject, TEST_PRINCIPAL_AWS_CONN_ID))
      .thenReturn(new AwsConnectionCredentials(new AwsCredentialsData() {
        @NotNull
        @Override
        public String getAccessKeyId() {
          return TEST_ACCESS_KEY_ID;
        }

        @NotNull
        @Override
        public String getSecretAccessKey() {
          return TEST_SECRET_ACCESS_KEY;
        }

        @Nullable
        @Override
        public String getSessionToken() {
          return null;
        }
      }, myAwsDefaultConnectionProperties));

    StaticCredentialsBuilder registeredStaticCredentialsBuilder = new StaticCredentialsBuilder(
      getAwsConnectorFactory(),
      Mockito.mock(AwsCredentialsFactory.class),
      getStsClientProvider(TEST_ACCESS_KEY_ID, TEST_SECRET_ACCESS_KEY, null)
    );

    IamRoleCredentialsBuilder registeredIamRoleCredentialsBuilder = new IamRoleCredentialsBuilder(
      getAwsConnectorFactory(),
      Mockito.mock(AwsCredentialsFactory.class),
      new LinkedAwsConnectionProviderImpl(myProjectManager, projectConnectionCredentialsManager),
      new AwsExternalIdsManagerImpl(myProjectManager),
      getStsClientProvider(TEST_IAM_ROLE_SESSION_ACCESS_KEY_ID, TEST_IAM_ROLE_SESSION_SECRET_ACCESS_KEY, TEST_IAM_ROLE_SESSION_TOKEN)
    );
  }

  @Test
  public void givenAwsConnFactory_whenPrincipalAwsConnIsRequested_thenReturnPrincipalAwsConnCredentials() {
    try {
      SProjectFeatureDescriptor connectionFeature = myProject.findFeatureById(TEST_PRINCIPAL_AWS_CONN_ID);
      assert connectionFeature != null;
      AwsCredentialsHolder credentialsHolder = getAwsConnectorFactory()
        .buildAwsCredentialsProvider(connectionFeature);
      assertEquals(TEST_ACCESS_KEY_ID, credentialsHolder.getAwsCredentials().getAccessKeyId());
      assertEquals(TEST_SECRET_ACCESS_KEY, credentialsHolder.getAwsCredentials().getSecretAccessKey());
      assertNull(credentialsHolder.getAwsCredentials().getSessionToken());

    } catch (ConnectionCredentialsException awsConnectorException) {
      fail("Could not construct the credentials provider: " + awsConnectorException.getMessage());
    }
  }

  @Test
  public void givenAwsConnFactory_whenWithAllProperties_thenReturnIamRoleSessionAwsCredentials() {
    try {
      SProjectFeatureDescriptor connectionFeature = myProject.findFeatureById(TEST_IAM_ROLE_AWS_CONN_ID);
      assert connectionFeature != null;
      AwsCredentialsHolder credentialsHolder = getAwsConnectorFactory()
        .buildAwsCredentialsProvider(connectionFeature);
      assertEquals(TEST_IAM_ROLE_SESSION_ACCESS_KEY_ID, credentialsHolder.getAwsCredentials().getAccessKeyId());
      assertEquals(TEST_IAM_ROLE_SESSION_SECRET_ACCESS_KEY, credentialsHolder.getAwsCredentials().getSecretAccessKey());
      assertEquals(TEST_IAM_ROLE_SESSION_TOKEN, credentialsHolder.getAwsCredentials().getSessionToken());

    } catch (ConnectionCredentialsException awsConnectorException) {
      fail("Could not construct the credentials provider: " + awsConnectorException.getMessage());
    }
  }

  public Map<String, String> createIamRoleConnectionProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    res.put(CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.IAM_ROLE_CREDENTIALS_TYPE);
    res.put(CHOSEN_AWS_CONN_ID_PARAM, TEST_PRINCIPAL_AWS_CONN_ID);
    res.put(AwsAssumeIamRoleParams.IAM_ROLE_ARN_PARAM, TEST_IAM_ROLE_ARN_PARAM);
    res.put(AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_PARAM, AwsAssumeIamRoleParams.IAM_ROLE_SESSION_NAME_DEFAULT);
    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
    return res;
  }

  @Override
  public Map<String, String> createConnectionDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(ACCESS_KEY_ID_PARAM, TEST_ACCESS_KEY_ID);
    res.put(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, TEST_SECRET_ACCESS_KEY);
    res.put(CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    res.put(AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
    res.put(SESSION_CREDENTIALS_PARAM, "false");
    return res;
  }
}