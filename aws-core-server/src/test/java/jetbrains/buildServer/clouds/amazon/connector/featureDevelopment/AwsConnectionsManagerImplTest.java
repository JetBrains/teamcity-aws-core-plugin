/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptorBuilder;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionDescriptorBuilderImpl;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionsEventsListener;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsConnectionsHolderImpl;
import jetbrains.buildServer.clouds.amazon.connector.common.impl.AwsCredentialsRefresheringManager;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.SESSION_DURATION_PARAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class AwsConnectionsManagerImplTest extends BaseTestCase {

  private final String testAccessKeyId = "TEST_ACCESS";
  private final String testSecretAccessKey = "TEST_SECRET";

  private final String testSessionAccessKeyId = "TEST_SESSION_ACCESS";
  private final String testSessionSecretAccessKey = "TEST_SESSION_SECRET";
  private final String testSessionToken = "TEST_SESSION_TOKEN";
  private final String testProjectId = "PROJECT_ID";
  private final String testConnectionId = "PROJECT_FEATURE_ID";
  private final String testConnectionDescription = "Test Connection";
  private AwsConnectorFactory myAwsConnectorFactory;
  private Map<String, String> myAwsDefaultConnectionProperties;

  private OAuthConnectionsManager myOAuthConnectionsManager;
  private SProject myProject;
  private ProjectManager myProjectManager;
  private CustomDataStorage myCustomDataStorage;
  private Map<String, String> myDataStorageValues;


  private AwsConnectionsManager myAwsConnectionsManager;

  private AwsConnectionDescriptorBuilder myAwsConnectionDescriptorBuilder;
  private AwsConnectionsHolderImpl myAwsConnectionsHolder;

  private AwsConnectionsEventsListener myAwsConnectionsEventsListener;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myAwsDefaultConnectionProperties = createDefaultProperties();
    myDataStorageValues = createDefaultStorageValues();

    initMainMocks();
    initMainTestObjects();

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory) {
      @Override
      @NotNull
      protected AwsCredentialsHolder createSessionCredentialsHolder(@NotNull final Map<String, String> cloudConnectorProperties) {

        return createTestCredentialsHolder();
      }
    };
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

  private void initMainTestObjects() {
    myAwsConnectorFactory = new AwsConnectorFactoryImpl(Mockito.mock(AwsConnectionIdGenerator.class));

    myAwsConnectionDescriptorBuilder = new AwsConnectionDescriptorBuilderImpl(
      myOAuthConnectionsManager,
      myAwsConnectorFactory
    );

    myAwsConnectionsHolder = new AwsConnectionsHolderImpl(
      myAwsConnectionDescriptorBuilder,
      myProjectManager,
      new AwsCredentialsRefresheringManager()
    );

    myAwsConnectionsEventsListener = new AwsConnectionsEventsListener(
      myAwsConnectionsHolder,
      myAwsConnectionDescriptorBuilder,
      (EventDispatcher<BuildServerListener>)Mockito.mock(EventDispatcher.class)
    );

    myAwsConnectionsManager = new AwsConnectionsManagerImpl(
      myAwsConnectionsHolder,
      myAwsConnectionDescriptorBuilder
    );
  }

  @Test
  public void givenAwsConnManager_whenRequestAwsConnById_thenReturnDefaultConnection() {
    try {
      AwsConnectionDescriptor awsConnectionDescriptor = myAwsConnectionsManager.getAwsConnection(testConnectionId);
      checkDefaultAwsConnProps(awsConnectionDescriptor);
      assertEquals(testSessionAccessKeyId, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

      assertTrue(awsConnectionDescriptor.isUsingSessionCredentials());
    } catch (AwsConnectorException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void givenAwsConnManager_whenWithTurnedOffSessionCredentialsAndNoSessionDurationParam_thenDontUseSessionCredentials() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    myAwsDefaultConnectionProperties.put(SESSION_CREDENTIALS_PARAM, "false");
    AwsCredentialsHolder credentialsHolder = new StaticCredentialsHolder(testAccessKeyId, testSecretAccessKey);
    SProjectFeatureDescriptor featureDescriptor = createTestAwsConnDescriptor(credentialsHolder, myAwsDefaultConnectionProperties);
    myAwsConnectionsEventsListener.projectFeatureChanged(
      myProject,
      featureDescriptor,
      featureDescriptor
    );

    try {
      AwsConnectionBean awsConnectionBean = myAwsConnectionsManager.getLinkedAwsConnection(someFeatureProps, myProject);
      checkDefaultAwsConnProps(awsConnectionBean);
      assertEquals(testAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());

      assertFalse(awsConnectionBean.isUsingSessionCredentials());

    } catch (LinkedAwsConnNotFoundException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  @Test
  public void givenAwsConnManager_whenWithSessionDurationParam_thenUseSessionCredentialsWithSpecifiedDuration() {

    final String testSessionDuration = "70";
    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);
    someFeatureProps.put(SESSION_DURATION_PARAM, testSessionDuration);

    try {
      AwsConnectionBean awsConnectionBean = myAwsConnectionsManager.getLinkedAwsConnection(someFeatureProps, myProject);
      checkDefaultAwsConnProps(awsConnectionBean);
      assertEquals(testSessionAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

      assertTrue(awsConnectionBean.isUsingSessionCredentials());

    } catch (LinkedAwsConnNotFoundException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  @Test
  public void givenAwsConnManager_whenWithoutSessionDurationParam_thenUseSessionCredentials() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    try {
      AwsConnectionBean awsConnectionBean = myAwsConnectionsManager.getLinkedAwsConnection(someFeatureProps, myProject);
      checkDefaultAwsConnProps(awsConnectionBean);
      assertEquals(testSessionAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

      assertTrue(awsConnectionBean.isUsingSessionCredentials());

    } catch (LinkedAwsConnNotFoundException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  private AwsConnectionDescriptor createTestAwsConnDescriptor(AwsCredentialsHolder credentialsHolder, Map<String, String> props) {
    return new AwsConnectionDescriptor() {
      @NotNull
      @Override
      public AwsCredentialsHolder getAwsCredentialsHolder() {
        return credentialsHolder;
      }

      @NotNull
      @Override
      public String getDescription() {
        return testConnectionDescription;
      }

      @NotNull
      @Override
      public String getRegion() {
        return REGION_NAME_DEFAULT;
      }

      @Override
      public boolean isUsingSessionCredentials() {
        return credentialsHolder.getAwsCredentials().getSessionToken() != null;
      }

      @NotNull
      @Override
      public String getProjectId() {
        return myProject.getProjectId();
      }

      @NotNull
      @Override
      public String getId() {
        return testConnectionId;
      }

      @NotNull
      @Override
      public String getType() {
        return OAuthConstants.FEATURE_TYPE;
      }

      @NotNull
      @Override
      public Map<String, String> getParameters() {
        return props;
      }
    };
  }

  private void checkDefaultAwsConnProps(AwsConnectionBean awsConnectionBean) {
    assertEquals(testConnectionId, awsConnectionBean.getConnectionId());
    assertEquals(REGION_NAME_DEFAULT, awsConnectionBean.getRegion());
  }

  private void checkDefaultAwsConnProps(AwsConnectionDescriptor awsConnectionDescriptor) {
    assertEquals(testConnectionId, awsConnectionDescriptor.getId());
    assertEquals(REGION_NAME_DEFAULT, awsConnectionDescriptor.getRegion());
  }

  private Map<String, String> createDefaultStorageValues() {
    Map<String, String> res = new HashMap<>();
    res.put(testConnectionId, testProjectId);
    return res;
  }

  private Map<String, String> createDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(OAuthConstants.OAUTH_TYPE_PARAM, AwsConnectionProvider.TYPE);
    res.put(ACCESS_KEY_ID_PARAM, testAccessKeyId);
    res.put(SECURE_SECRET_ACCESS_KEY_PARAM, testSecretAccessKey);
    res.put(CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE);
    res.put(REGION_NAME_PARAM, REGION_NAME_DEFAULT);
    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
    return res;
  }

  private AwsCredentialsHolder createTestCredentialsHolder() {
    return new AwsCredentialsHolder() {
      @NotNull
      @Override
      public Date getSessionExpirationDate() {
        return new Date();
      }

      @NotNull
      @Override
      public AwsCredentialsData getAwsCredentials() {
        return new AwsCredentialsData() {
          @NotNull
          @Override
          public String getAccessKeyId() {
            return testSessionAccessKeyId;
          }

          @NotNull
          @Override
          public String getSecretAccessKey() {
            return testSessionSecretAccessKey;
          }

          @Override
          public String getSessionToken() {
            return testSessionToken;
          }
        };
      }

      @Override
      public void refreshCredentials() {
        //...
      }
    };
  }
}