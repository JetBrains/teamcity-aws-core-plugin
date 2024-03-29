package jetbrains.buildServer.clouds.amazon.connector.testUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionDescriptorBuilderImpl;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionsEventsListener;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionsHolderImpl;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsCredentialsRefresheringManager;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManagerImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.EventDispatcher;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public abstract class AbstractAwsConnectionTest extends BaseTestCase {

  protected final String testProjectId = "PROJECT_ID";
  protected final String testConnectionId = "PROJECT_FEATURE_ID";

  protected final String testConnectionParam = "SOME_CONNECTION_PARAM";
  protected final String testConnectionDescription = "Test Connection";
  private final Map<String, SProject> mockedProjectsCollection = new HashMap<>();
  private final Map<String, OAuthConnectionDescriptor> mockedAwsConnectionsCollection = new HashMap<>();
  protected Map<String, String> myAwsDefaultConnectionProperties;
  protected OAuthConnectionsManager myOAuthConnectionsManager;
  protected ProjectManager myProjectManager;
  protected OAuthProvider myAwsOauthProvider;
  protected Map<String, String> myDataStorageValues;
  private AwsConnectorFactory myAwsConnectorFactory = null;
  private AwsConnectionsManager myAwsConnectionsManager = null;
  private AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder = null;
  private AwsConnectionsHolderImpl myAwsConnectionsHolder = null;
  private AwsConnectionIdGenerator myAwsConnectionIdGenerator = null;
  private AwsConnectionsEventsListener myAwsConnectionsEventsListener = null;

  @Override
  @BeforeMethod(alwaysRun = true)
  protected void setUp() throws Exception {
    super.setUp();
    initAwsConnectionTester();
  }

  public void initAwsConnectionTester() {
    myAwsConnectorFactory = null;
    myAwsConnectionsManager = null;
    myAwsConnectionDescriptorBuilder = null;
    myAwsConnectionsHolder = null;
    myAwsConnectionIdGenerator = null;
    myAwsConnectionsEventsListener = null;

    initMainComponents();
  }

  public abstract Map<String, String> createConnectionDefaultProperties();

  protected Map<String, String> createDefaultStorageValues() {
    return Collections.emptyMap();
  }


  public void addTeamCityAwsConnection(SProject featureOwner, SProjectFeatureDescriptor feature) {
    OAuthConnectionDescriptor awsConnDescriptor = Mockito.mock(OAuthConnectionDescriptor.class);
    when(awsConnDescriptor.getParameters())
      .thenReturn(feature.getParameters());
    when(awsConnDescriptor.getId())
      .thenReturn(feature.getId());
    when(awsConnDescriptor.getProject())
      .thenReturn(featureOwner);
    when(awsConnDescriptor.getDescription())
      .thenReturn(testConnectionDescription);
    when(awsConnDescriptor.getOauthProvider())
      .thenReturn(myAwsOauthProvider);

    when(featureOwner.findFeatureById(feature.getId()))
      .thenReturn(feature);

    mockedAwsConnectionsCollection.put(awsConnDescriptor.getId(), awsConnDescriptor);
  }

  public void removeTeamCityAwsConnection(String awsConnectionId) {
    mockedAwsConnectionsCollection.remove(awsConnectionId);
  }


  public void addTeamCityProject(SProject project) {
    mockedProjectsCollection.put(project.getProjectId(), project);
  }

  public void removeTeamCityProject(SProject project) {
    mockedProjectsCollection.remove(project.getProjectId());
  }


  public AwsConnectorFactory getAwsConnectorFactory() {
    if (myAwsConnectorFactory == null) {
      myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    }
    return myAwsConnectorFactory;
  }

  public AwsConnectionsManager getAwsConnectionsManager() {
    if (myAwsConnectionsManager == null) {
      myAwsConnectionsManager = new AwsConnectionsManagerImpl(
        getAwsConnectionsHolder(),
        getAwsConnectionDescriptorBuilder()
      );
    }
    return myAwsConnectionsManager;
  }

  public AwsConnectionDescriptorBuilder getAwsConnectionDescriptorBuilder() {
    if (myAwsConnectionDescriptorBuilder == null) {
      myAwsConnectionDescriptorBuilder = new AwsConnectionDescriptorBuilderImpl(
        myOAuthConnectionsManager,
        getAwsConnectorFactory(),
        Mockito.mock(ExtensionHolder.class)
      );
    }
    return myAwsConnectionDescriptorBuilder;
  }

  public AwsConnectionsHolderImpl getAwsConnectionsHolder() {
    if (myAwsConnectionsHolder == null) {
      myAwsConnectionsHolder = new AwsConnectionsHolderImpl(
        getAwsConnectionDescriptorBuilder(),
        myProjectManager,
        new AwsCredentialsRefresheringManager()
      );
    }
    return myAwsConnectionsHolder;
  }

  public AwsConnectionIdGenerator getAwsConnectionIdGenerator() {
    if (myAwsConnectionIdGenerator == null) {
      myAwsConnectionIdGenerator = new AwsConnectionIdGenerator(
        getAwsConnectionsHolder(),
        Mockito.mock(OAuthConnectionsIdGenerator.class)
      );
    }
    return myAwsConnectionIdGenerator;
  }

  public AwsConnectionsEventsListener getAwsConnectionsEventsListener() {
    if (myAwsConnectionsEventsListener == null) {
      myAwsConnectionsEventsListener = new AwsConnectionsEventsListener(
        getAwsConnectionsHolder(),
        getAwsConnectionDescriptorBuilder(),
        (EventDispatcher<BuildServerListener>)Mockito.mock(EventDispatcher.class)
      );
    }
    return myAwsConnectionsEventsListener;
  }


  public SProject getMockedProject(String projectId, Map<String, String> dataStorageValues) {
    SProject project = Mockito.mock(SProject.class);
    when(project.getProjectId())
      .thenReturn(projectId);

    CustomDataStorage customDataStorage = getMockedDataStorage(dataStorageValues);
    when(project.getCustomDataStorage(anyString()))
      .thenReturn(customDataStorage);
    return project;
  }

  private CustomDataStorage getMockedDataStorage(Map<String, String> dataStorageValues) {
    CustomDataStorage customDataStorage = Mockito.mock(CustomDataStorage.class);

    when(customDataStorage.getValues())
      .thenReturn(dataStorageValues);

    doAnswer(invocation -> {
      Map<String, String> updatedValues = invocation.getArgument(0);
      Set<String> removedKeys = invocation.getArgument(1);
      removedKeys.forEach(removedKey -> myDataStorageValues.remove(removedKey));
      myDataStorageValues.putAll(updatedValues);
      return null;
    }).when(customDataStorage).updateValues(any(), any());

    doAnswer(invocation -> {
      myDataStorageValues.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(customDataStorage).putValue(any(), any());

    return customDataStorage;
  }


  private void initMainComponents() {
    myAwsOauthProvider = Mockito.mock(AwsConnectionProvider.class);
    when(myAwsOauthProvider.getType())
      .thenReturn(AwsConnectionProvider.TYPE);

    myAwsDefaultConnectionProperties = createConnectionDefaultProperties();
    myDataStorageValues = createDefaultStorageValues();

    initMainMocks();
  }

  private void initMainMocks() {
    initProjectManagerMock();
    initOauthConnManagerMock();
  }

  private void initProjectManagerMock() {
    myProjectManager = Mockito.mock(ProjectManager.class);
    SProject rootProject = getMockedProject("Root", myDataStorageValues);
    when(myProjectManager.getRootProject())
      .thenReturn(rootProject);
    when(myProjectManager.findProjectById(anyString()))
      .thenAnswer(invocation -> mockedProjectsCollection.get(invocation.getArgument(0)));
  }

  private void initOauthConnManagerMock() {
    myOAuthConnectionsManager = Mockito.mock(OAuthConnectionsManager.class);

    when(myOAuthConnectionsManager.findConnectionById(any(), anyString()))
      .thenAnswer(invocation -> mockedAwsConnectionsCollection.get(invocation.getArgument(1)));
  }
}
