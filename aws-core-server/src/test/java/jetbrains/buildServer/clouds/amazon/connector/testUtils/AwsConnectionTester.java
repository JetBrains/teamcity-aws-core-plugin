package jetbrains.buildServer.clouds.amazon.connector.testUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jetbrains.buildServer.BaseTestCase;
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
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.EventDispatcher;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public abstract class AwsConnectionTester extends BaseTestCase {

  protected final String testProjectId = "PROJECT_ID";
  protected final String testConnectionId = "PROJECT_FEATURE_ID";
  protected final String testConnectionDescription = "Test Connection";
  protected Map<String, String> myAwsDefaultConnectionProperties;

  protected OAuthConnectionsManager myOAuthConnectionsManager;
  protected SProject myProject;
  protected ProjectManager myProjectManager;
  protected CustomDataStorage myCustomDataStorage;
  protected Map<String, String> myDataStorageValues;


  private AwsConnectorFactory myAwsConnectorFactory = null;
  private AwsConnectionsManager myAwsConnectionsManager = null;
  private AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder = null;
  private AwsConnectionsHolderImpl myAwsConnectionsHolder = null;
  private AwsConnectionsEventsListener myAwsConnectionsEventsListener = null;

  public void initAwsConnectionTester() {
    myAwsConnectorFactory = null;
    myAwsConnectionsManager = null;
    myAwsConnectionDescriptorBuilder = null;
    myAwsConnectionsHolder = null;
    myAwsConnectionsEventsListener = null;
    initMainComponents();
  }

  protected abstract Map<String, String> createConnectionDefaultProperties();

  protected abstract Map<String, String> createDefaultStorageValues();

  public AwsConnectorFactory getAwsConnectorFactory() {
    if (myAwsConnectorFactory == null) {
      myAwsConnectorFactory = new AwsConnectorFactoryImpl(new AwsConnectionIdGenerator(Mockito.mock(OAuthConnectionsIdGenerator.class)));
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
        getAwsConnectorFactory()
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


  private void initMainComponents() {
    myAwsDefaultConnectionProperties = createConnectionDefaultProperties();
    myDataStorageValues = createDefaultStorageValues();
    initMainMocks();
  }

  private void initMainMocks() {
    initProjectMock();
    initProjectManagerMock();
    initOauthConnManagerMock();
  }

  private void initProjectMock() {
    initDataStorageMock();

    myProject = Mockito.mock(SProject.class);
    when(myProject.getProjectId())
      .thenReturn(testProjectId);
    when(myProject.getCustomDataStorage(any()))
      .thenReturn(myCustomDataStorage);
  }

  private void initDataStorageMock() {
    myCustomDataStorage = Mockito.mock(CustomDataStorage.class);

    when(myCustomDataStorage.getValues())
      .thenReturn(myDataStorageValues);

    doAnswer(invocation -> {
      Set<String> removedKey = new HashSet<>();
      removedKey.add(testConnectionId);
      assertEquals(removedKey, invocation.getArgument(1));
      myDataStorageValues.remove(testConnectionId);
      return null;

    }).when(myCustomDataStorage).updateValues(any(), any());

    doAnswer(invocation -> {
      assertEquals(testConnectionId, invocation.getArgument(0));
      assertEquals(testProjectId, invocation.getArgument(1));
      return null;

    }).when(myCustomDataStorage).putValue(any(), any());
  }

  private void initProjectManagerMock() {
    myProjectManager = Mockito.mock(ProjectManager.class);
    when(myProjectManager.getRootProject())
      .thenReturn(myProject);
    when(myProjectManager.findProjectById(testProjectId))
      .thenReturn(myProject);
  }

  private void initOauthConnManagerMock() {
    OAuthConnectionDescriptor awsConnDescriptor = Mockito.mock(OAuthConnectionDescriptor.class);
    when(awsConnDescriptor.getParameters())
      .thenReturn(myAwsDefaultConnectionProperties);
    when(awsConnDescriptor.getId())
      .thenReturn(testConnectionId);
    when(awsConnDescriptor.getProject())
      .thenReturn(myProject);
    when(awsConnDescriptor.getDescription())
      .thenReturn(testConnectionDescription);

    OAuthProvider testAwsOauthProvider = Mockito.mock(AwsConnectionProvider.class);
    when(testAwsOauthProvider.getType())
      .thenReturn(AwsConnectionProvider.TYPE);
    when(awsConnDescriptor.getOauthProvider())
      .thenReturn(testAwsOauthProvider);


    myOAuthConnectionsManager = Mockito.mock(OAuthConnectionsManager.class);
    when(myOAuthConnectionsManager.findConnectionById(myProject, testConnectionId))
      .thenReturn(awsConnDescriptor);
  }
}
