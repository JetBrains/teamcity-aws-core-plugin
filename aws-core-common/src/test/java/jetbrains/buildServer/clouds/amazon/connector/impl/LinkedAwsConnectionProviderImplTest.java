package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.SimpleParameter;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

public class LinkedAwsConnectionProviderImplTest extends BaseServerTestCase {

  private ProjectConnectionsManager myProjectConnectionCredentialsManager;
  private LinkedAwsConnectionProviderImpl myLinkedAwsConnectionProvider;
  private ProjectEx myChildProject;

  private BuildTypeEx myBuildTypeEx;

  private final String CONNECTION_ID = "connectionId";

  @BeforeMethod(alwaysRun = true)
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myProjectConnectionCredentialsManager = Mockito.mock(ProjectConnectionsManager.class);
    myChildProject = myProject.createProject("ChildProject", "Child Project");

    myBuildTypeEx = myChildProject.createBuildType("childBuildType");
    myBuildTypeEx.addBuildFeature(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE,
                              ImmutableMap.of(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, CONNECTION_ID));
    myProject.addParameter(new SimpleParameter(AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_FEATURE_FLAG, "true"));
    myProject.persist();

    myLinkedAwsConnectionProvider = new LinkedAwsConnectionProviderImpl(myProjectManager, myProjectConnectionCredentialsManager, Mockito.mock(ProjectConnectionCredentialsManager.class));
  }

  private void testWithParamIsDisabled(String allowedInSubprojectsParam) throws ConnectionCredentialsException {
    ConnectionDescriptor descriptor = Mockito.mock(ConnectionDescriptor.class);
    Mockito.when(descriptor.getProjectId()).thenReturn(myProject.getProjectId());
    Mockito.when(descriptor.getParameters()).thenReturn(ImmutableMap.of(allowedInSubprojectsParam, "false"));
    Mockito.when(myProjectConnectionCredentialsManager.findConnectionById(myChildProject, CONNECTION_ID)).thenReturn(descriptor);
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
}