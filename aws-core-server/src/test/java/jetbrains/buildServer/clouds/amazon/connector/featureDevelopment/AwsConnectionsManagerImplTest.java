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

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.errors.features.LinkedAwsConnNotFoundException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static org.mockito.Mockito.when;

public class AwsConnectionsManagerImplTest extends BaseTestCase {

  private final String testAccessKeyId = "TESTACCESS";
  private final String testSecretAccessKey = "TESTSECRET";
  private final String testConnectionId = "PROJECT_FEATURE_ID";
  private final String testConnectionDescription = "Test Conneciton";
  private AwsConnectorFactory myAwsConnectorFactory;
  private Map<String, String> myAwsConnectionProperties;
  private ExecutorServices myExecutorServices;

  private AwsConnectionsManager myAwsConnectionsManager;
  private SProject myProject;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();
    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    myAwsConnectionProperties = createDefaultProperties();
    myExecutorServices = Mockito.mock(ExecutorServices.class);
    myProject = Mockito.mock(SProject.class);

    OAuthConnectionDescriptor awsConnDescriptor = Mockito.mock(OAuthConnectionDescriptor.class);
    when(awsConnDescriptor.getParameters())
      .thenReturn(myAwsConnectionProperties);
    when(awsConnDescriptor.getId())
      .thenReturn(testConnectionId);
    when(awsConnDescriptor.getDescription())
      .thenReturn(testConnectionDescription);

    OAuthConnectionsManager oAuthConnectionsManager = Mockito.mock(OAuthConnectionsManager.class);
    when(oAuthConnectionsManager.findConnectionById(myProject, testConnectionId))
      .thenReturn(awsConnDescriptor);

    myAwsConnectionsManager = new AwsConnectionsManagerImpl(oAuthConnectionsManager, myAwsConnectorFactory);

    StaticCredentialsBuilder staticCredentialsFactory = new StaticCredentialsBuilder(myAwsConnectorFactory, myExecutorServices);
  }

  @Test
  public void givenAwsConnManager_whenWithTurnedOffSessionCredentialsAndNoSessionDurationParam_thenDontUseSessionCredentials() {

    Map<String, String> someFeatureProps = new HashMap<>();
    someFeatureProps.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, testConnectionId);

    myAwsConnectionProperties.put(SESSION_CREDENTIALS_PARAM, "false");

    try {
      AwsConnectionBean awsConnectionBean = myAwsConnectionsManager.getLinkedAwsConnection(someFeatureProps, myProject);
      assert awsConnectionBean != null;
      checkDefaultAwsConnProps(awsConnectionBean);

      assertFalse(awsConnectionBean.isUsingSessionCredentials());

    } catch (LinkedAwsConnNotFoundException e) {
      fail("Could not find linked aws connection: " + e.getMessage());
    }
  }

  private void checkDefaultAwsConnProps(AwsConnectionBean awsConnectionBean) {
    assertEquals(testAccessKeyId, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getAccessKeyId());
    assertEquals(testSecretAccessKey, awsConnectionBean.getAwsCredentialsHolder().getAwsCredentials().getSecretAccessKey());
    assertEquals(testConnectionId, awsConnectionBean.getConnectionId());
    assertEquals(testConnectionDescription, awsConnectionBean.getDescription());
    assertEquals(REGION_NAME_DEFAULT, awsConnectionBean.getRegion());
  }

  private Map<String, String> createDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(ACCESS_KEY_ID_PARAM, testAccessKeyId);
    res.put(SECURE_SECRET_ACCESS_KEY_PARAM, testSecretAccessKey);
    res.put(CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE);
    res.put(REGION_NAME_PARAM, REGION_NAME_DEFAULT);
    res.put(SESSION_CREDENTIALS_PARAM, "false");
    return res;
  }
}