package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.*;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.SimpleParameter;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnectionCredentialsConstants.*;
import static org.assertj.core.api.BDDAssertions.then;

public class LinkedAwsConnectionProviderImplTest extends BaseServerTestCase {

  private ProjectConnectionsManager myProjectConnectionsManager;
  private ProjectConnectionCredentialsManager myProjectConnectionCredentialsManager;
  private LinkedAwsConnectionProviderImpl myLinkedAwsConnectionProvider;
  private ProjectEx myChildProject;

  private BuildTypeEx myBuildTypeEx;

  private final String CONNECTION_ID = "connectionId";

  @BeforeMethod(alwaysRun = true)
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myProjectConnectionsManager = Mockito.mock(ProjectConnectionsManager.class);
    myProjectConnectionCredentialsManager = Mockito.mock(ProjectConnectionCredentialsManager.class);
    myChildProject = myProject.createProject("ChildProject", "Child Project");

    myBuildTypeEx = myChildProject.createBuildType("childBuildType");
    myBuildTypeEx.addBuildFeature(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE,
                              ImmutableMap.of(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, CONNECTION_ID));
    myProject.addParameter(new SimpleParameter(AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_FEATURE_FLAG, "true"));
    myProject.persist();

    myLinkedAwsConnectionProvider = new LinkedAwsConnectionProviderImpl(myProjectManager, myProjectConnectionsManager, myProjectConnectionCredentialsManager);
  }

  private void testWithParamIsDisabled(String allowedInSubprojectsParam) throws ConnectionCredentialsException {
    ConnectionDescriptor descriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(descriptor.getProjectId()).thenReturn(myProject.getProjectId());
    Mockito.when(descriptor.getParameters()).thenReturn(ImmutableMap.of(allowedInSubprojectsParam, "false"));
    Mockito.when(myProjectConnectionsManager.findConnectionById(myChildProject, CONNECTION_ID)).thenReturn(descriptor);
    SRunningBuild build = createRunningBuild(myBuildType, new String[0], new String[0]);


    List<ConnectionCredentials> connectionCredentialsFromBuild = myLinkedAwsConnectionProvider.getConnectionCredentialsFromBuild(build);
    then(connectionCredentialsFromBuild).isEmpty();
  }

  @Test
  public void testUsingConnectionWhenAllowingSubprojectsIsDisabled() throws ConnectionCredentialsException {
    testWithParamIsDisabled(AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_PARAM);
  }

  @Test
  public void testUsingConnectionWhenAllowingInBuildIsDisabled() throws ConnectionCredentialsException {
    testWithParamIsDisabled(AwsCloudConnectorConstants.ALLOWED_IN_BUILDS_REQUEST_PARAM);
  }

  @Test(expectedExceptions = AwsConnectorException.class, expectedExceptionsMessageRegExp = ".*Cannot find the Project with ID.*")
  void testProjectNotFound() throws ConnectionCredentialsException {

    String nonExistingProject = myProject.getProjectId() + UUID.randomUUID();
    String awsConnectionId = UUID.randomUUID().toString();
    AwsConnectionParameters awsConnectionParameters = AwsConnectionParameters.AwsConnectionParametersBuilder.of(awsConnectionId)
        .withInternalProjectId(nonExistingProject)
          .build();

    myLinkedAwsConnectionProvider.getAwsCredentialsProvider(awsConnectionParameters);
  }

  @Test(expectedExceptions = ConnectionCredentialsException.class, expectedExceptionsMessageRegExp = ".*Invalid linked AWS connection: This connection is not supported.*")
  void testWrongTypeOfConnectionCredentials() throws ConnectionCredentialsException {

    String awsConnectionId = UUID.randomUUID().toString();
    ConnectionDescriptor connectionDescriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(connectionDescriptor.getId()).thenReturn(awsConnectionId);
    Mockito.when(myProjectConnectionsManager.findConnectionById(Mockito.any(SProject.class), Mockito.anyString())).thenReturn(connectionDescriptor);

    Mockito.when(myProjectConnectionCredentialsManager.requestConnectionCredentials(Mockito.any(SProject.class), Mockito.anyString(), Mockito.anyMap()))
      .thenReturn(new ConnectionCredentials() {
        @NotNull
        @Override
        public Map<String, String> getProperties() {
          return getTestAwsConnectionProperties();
        }

        @NotNull
        @Override
        public String getProviderType() {
          return "";
        }
      });

    AwsConnectionParameters awsConnectionParameters = AwsConnectionParameters.AwsConnectionParametersBuilder.of(awsConnectionId)
      .withInternalProjectId(myProject.getProjectId())
      .build();

    myLinkedAwsConnectionProvider.getAwsCredentialsProvider(awsConnectionParameters);
  }

  @Test
  void testGetAwsCredentialsProviderByAwsConnectionParameters() throws ConnectionCredentialsException {
    String awsConnectionId = UUID.randomUUID().toString();

    ConnectionDescriptor connectionDescriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(connectionDescriptor.getId()).thenReturn(awsConnectionId);

    AwsConnectionCredentials connectionCredentials = Mockito.mock(AwsConnectionCredentials.class);
    Mockito.when(connectionCredentials.getProperties())
      .thenReturn(getTestAwsConnectionProperties());
    Mockito.when(connectionCredentials.getProviderType())
      .thenReturn(AwsCloudConnectorConstants.CLOUD_TYPE);
    Mockito.when(connectionCredentials.toAWSCredentialsProvider()).thenReturn(Mockito.mock(AWSCredentialsProvider.class));
    Mockito.when(myProjectConnectionsManager.findConnectionById(Mockito.any(SProject.class), Mockito.anyString())).thenReturn(connectionDescriptor);

    Mockito.when(myProjectConnectionCredentialsManager.requestConnectionCredentials(Mockito.any(SProject.class), Mockito.anyString(), Mockito.anyMap()))
      .thenReturn(connectionCredentials);

    AwsConnectionParameters awsConnectionParameters = AwsConnectionParameters.AwsConnectionParametersBuilder.of(awsConnectionId)
      .withInternalProjectId(myProject.getProjectId())
      .build();

    Assertions.assertThat(myLinkedAwsConnectionProvider.getAwsCredentialsProvider(awsConnectionParameters)).isNotNull();
  }

  public static Map<String, String> getTestAwsConnectionProperties() {
    Map<String, String> properties = new HashMap<>();
    String TEST_ACCESS_KEY_ID = "TEST_ACCESS_KEY_ID";
    String TEST_SECRET_ACCESS_KEY = "TEST_SECRET_ACCESS_KEY";
    String TEST_SESSION_TOKEN = "TEST_SESSION_TOKEN";
    String TEST_AWS_REGION = "eu-west-1";

    properties.put(ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
    properties.put(SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
    properties.put(SESSION_TOKEN, TEST_SESSION_TOKEN);
    properties.put(REGION, TEST_AWS_REGION);
    return properties;
  }
}