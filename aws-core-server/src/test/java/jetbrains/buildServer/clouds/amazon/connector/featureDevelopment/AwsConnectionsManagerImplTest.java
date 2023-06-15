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

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.testUtils.AbstractAwsConnectionTest;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.serverSide.connections.aws.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProvider;

public class AwsConnectionsManagerImplTest extends AbstractAwsConnectionTest {

  private final String testAccessKeyId = "TEST_ACCESS";
  private final String testSecretAccessKey = "TEST_SECRET";

  private final String testSessionAccessKeyId = "TEST_SESSION_ACCESS";
  private final String testSessionSecretAccessKey = "TEST_SESSION_SECRET";
  private final String testSessionToken = "TEST_SESSION_TOKEN";

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
      getStsClientProvider(
        testSessionAccessKeyId,
        testSessionSecretAccessKey,
        testSessionToken
      )
    );
  }

  @Test
  public void givenAwsConnManager_whenRequestAwsConnById_thenReturnDefaultConnection() {
    try {
      AwsConnectionDescriptor awsConnectionDescriptor = getAwsConnectionsHolder().getAwsConnection(testConnectionId);
      checkDefaultAwsConnProps(awsConnectionDescriptor);
      assertEquals(testSessionAccessKeyId, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnectionDescriptor.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

    } catch (ConnectionCredentialsException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void newApi_givenAwsConnManager_whenWithTurnedOffSessionCredentialsAndNoSessionDurationParam_thenDontUseSessionCredentials() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    myAwsDefaultConnectionProperties.put(SESSION_CREDENTIALS_PARAM, "false");
    AwsCredentialsHolder credentialsHolder = new StaticCredentialsHolder(testAccessKeyId, testSecretAccessKey);
    SProjectFeatureDescriptor featureDescriptor = createTestAwsConnDescriptor(credentialsHolder, myAwsDefaultConnectionProperties);
    getAwsConnectionsEventsListener().projectFeatureChanged(
      myProject,
      featureDescriptor,
      featureDescriptor
    );

    try {
      AwsConnectionDescriptor awsConnection = getAwsConnectionsManager().getLinkedAwsConnection(someFeatureProps);
      checkDefaultAwsConnProps(awsConnection);
      assertEquals(testAccessKeyId, awsConnection.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSecretAccessKey, awsConnection.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());

    } catch (ConnectionCredentialsException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  @Test
  public void newApi_givenAwsConnManager_getLinkedAwsConnectionWhenWithoutSessionDurationParam_thenUseSessionCredentialsByDefault() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    try {
      AwsConnectionDescriptor awsConnection = getAwsConnectionsManager().getLinkedAwsConnection(someFeatureProps);
      checkDefaultAwsConnProps(awsConnection);
      assertEquals(testSessionAccessKeyId, awsConnection.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnection.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnection.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

    } catch (ConnectionCredentialsException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }


  //Deprecated methods
  @Test
  public void givenAwsConnManager_whenWithTurnedOffSessionCredentialsAndNoSessionDurationParam_thenDontUseSessionCredentials() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    myAwsDefaultConnectionProperties.put(SESSION_CREDENTIALS_PARAM, "false");
    AwsCredentialsHolder credentialsHolder = new StaticCredentialsHolder(testAccessKeyId, testSecretAccessKey);
    SProjectFeatureDescriptor featureDescriptor = createTestAwsConnDescriptor(credentialsHolder, myAwsDefaultConnectionProperties);
    getAwsConnectionsEventsListener().projectFeatureChanged(
      myProject,
      featureDescriptor,
      featureDescriptor
    );

    try {
      AwsConnectionBean awsConnectionBean = getAwsConnectionsManager().getLinkedAwsConnection(someFeatureProps, myProject);
      checkDefaultAwsConnProps(awsConnectionBean);
      assertEquals(testAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());

      assertFalse(awsConnectionBean.isUsingSessionCredentials());

    } catch (ConnectionCredentialsException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  @Test
  public void givenAwsConnManager_getLinkedAwsConnectionWhenWithoutSessionDurationParam_thenUseSessionCredentialsByDefault() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    try {
      AwsConnectionBean awsConnectionBean = getAwsConnectionsManager().getLinkedAwsConnection(someFeatureProps, myProject);
      checkDefaultAwsConnProps(awsConnectionBean);
      assertEquals(testSessionAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
      assertEquals(testSessionSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
      assertEquals(testSessionToken, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSessionToken());

      assertTrue(awsConnectionBean.isUsingSessionCredentials());

    } catch (ConnectionCredentialsException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  private AwsConnectionDescriptor createTestAwsConnDescriptor(AwsCredentialsHolder credentialsHolder, Map<String, String> props) {
    return new AwsConnectionDescriptor() {
      @NotNull
      @Override
      public String getDisplayName() {
        return "";
      }

      @NotNull
      @Override
      public ConnectionProvider getConnectionProvider() {
        return new AwsConnectionProvider(Mockito.mock(PluginDescriptor.class), Mockito.mock(AwsConnectionCredentialsFactory.class));
      }

      @NotNull
      @Override
      public AwsCredentialsHolder getAwsCredentialsHolder() {
        return credentialsHolder;
      }

      @NotNull
      @Override
      public String getRegion() {
        return REGION_NAME_DEFAULT;
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

  @Override
  protected Map<String, String> createDefaultStorageValues() {
    Map<String, String> res = new HashMap<>();
    res.put(testConnectionId, testProjectId);
    return res;
  }

  @Override
  public Map<String, String> createConnectionDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(OAuthConstants.OAUTH_TYPE_PARAM, AwsConnectionProvider.TYPE);
    res.put(ACCESS_KEY_ID_PARAM, testAccessKeyId);
    res.put(SECURE_SECRET_ACCESS_KEY_PARAM, testSecretAccessKey);
    res.put(CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE);
    res.put(REGION_NAME_PARAM, REGION_NAME_DEFAULT);
    res.put(STS_ENDPOINT_PARAM, STS_ENDPOINT_DEFAULT);
    return res;
  }
}