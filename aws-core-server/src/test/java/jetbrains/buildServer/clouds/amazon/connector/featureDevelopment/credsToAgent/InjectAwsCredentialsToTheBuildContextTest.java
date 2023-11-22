/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import com.intellij.util.containers.hash.HashMap;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.LinkedAwsConnectionProviderImpl;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import jetbrains.buildServer.serverSide.impl.BuildFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnectionCredentialsConstants.*;
import static jetbrains.buildServer.testUtils.TestUtils.createConnectionDescriptor;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class InjectAwsCredentialsToTheBuildContextTest {
  private final String TEST_AWS_CONNECTION_ID = "TEST_AWS_CONNECTION_ID";
  private final String TEST_ACCESS_KEY_ID = "TEST_ACCESS_KEY_ID";
  private final String TEST_SECRET_ACCESS_KEY = "TEST_SECRET_ACCESS_KEY";
  private final String TEST_SESSION_TOKEN = "TEST_SESSION_TOKEN";
  private final String TEST_AWS_REGION = "eu-west-1";

  private InjectAwsCredentialsToTheBuildContext injectAwsCredentials;

  private BuildStartContext mockedBuildStartContext;
  private ExtensionHolder mockedExtensionHolder;

  @BeforeMethod
  public void setup() throws Exception {
    mockedExtensionHolder = Mockito.mock(ExtensionHolder.class);

    ProjectManager mockedProjectManager = Mockito.mock(ProjectManager.class, RETURNS_DEEP_STUBS);
    ProjectConnectionsManager mockedConnectionsManager = Mockito.mock(ProjectConnectionsManager.class, RETURNS_DEEP_STUBS);
    ProjectConnectionCredentialsManager mockedConnectionCredentialsManager = Mockito.mock(ProjectConnectionCredentialsManager.class, RETURNS_DEEP_STUBS);

    LinkedAwsConnectionProvider linkedAwsConnectionProvider = new LinkedAwsConnectionProviderImpl(
      mockedProjectManager,
      mockedConnectionsManager,
      mockedConnectionCredentialsManager
    );
    injectAwsCredentials = new InjectAwsCredentialsToTheBuildContext(linkedAwsConnectionProvider);

    mockedBuildStartContext = Mockito.mock(BuildStartContext.class, RETURNS_DEEP_STUBS);

    Map<String, String> buildFeatureProperties = new HashMap<>();
    buildFeatureProperties.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, TEST_AWS_CONNECTION_ID);
    SRunningBuild runningBuild = Mockito.mock(SRunningBuild.class, RETURNS_DEEP_STUBS);
    BuildTypeEx buildType = Mockito.mock(BuildTypeEx.class);
    when(mockedBuildStartContext.getBuild()).thenReturn(runningBuild);
    when(runningBuild.getBuildType()).thenReturn(buildType);
    when(runningBuild.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE))
      .thenReturn(
        Collections.singletonList(
          new BuildFeatureDescriptorImpl(
            "INJECT_AWS_CONN_TO_BUILD_ID",
            AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE,
            buildFeatureProperties,
            mockedExtensionHolder
          )
        )
      );

    ConnectionDescriptor mockedConnectionDescriptor = createConnectionDescriptor(
      "PROJECT_ID",
      TEST_AWS_CONNECTION_ID,
      Collections.emptyMap()
    );
    when(mockedConnectionsManager.findConnectionById(any(), any()))
      .thenReturn(mockedConnectionDescriptor);

    when(mockedConnectionCredentialsManager.requestConnectionCredentials(any(), eq(TEST_AWS_CONNECTION_ID)))
      .thenReturn(
        new ConnectionCredentials() {
          @NotNull
          @Override
          public Map<String, String> getProperties() {
            Map<String, String> creds = new HashMap<>();
            creds.put(ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            creds.put(SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
            creds.put(SESSION_TOKEN, TEST_SESSION_TOKEN);
            creds.put(REGION, TEST_AWS_REGION);
            return creds;
          }

          @NotNull
          @Override
          public String getProviderType() {
            return AwsConnectionProvider.TYPE;
          }
        }
      );
  }

  @Test
  void given_aws_conn_with_credentials_then_inject_correct_file_contents() {
    ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
    doAnswer(invocation -> {
      if (AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT.equals(arg1.getValue())) {
        assertTrue(credentialsCorrectlyEncoded(arg2.getValue()));
      }
      return null;
    }).when(mockedBuildStartContext).addSharedParameter(arg1.capture(), arg2.capture());

    injectAwsCredentials.updateParameters(mockedBuildStartContext);

    verify(mockedBuildStartContext).addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, TEST_ACCESS_KEY_ID);
  }

  @Test
  void given_no_aws_credentials_buildFeature_then_do_nothing() {
    when(mockedBuildStartContext.getBuild().getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE))
      .thenReturn(
        Collections.emptyList()
      );

    injectAwsCredentials.updateParameters(mockedBuildStartContext);

    verify(mockedBuildStartContext, times(0)).addSharedParameter(any(), any());
  }

  private boolean credentialsCorrectlyEncoded(String actualEncodedCredentials) {
    byte[] decodedBytes = Base64.getDecoder().decode(actualEncodedCredentials);
    String decodedCredentials = new String(decodedBytes, StandardCharsets.UTF_8);

    List<String> expectedAwsCredentialsValues = Arrays.asList(
      String.format("%s=%s", AWS_ACCESS_KEY_CONFIG_FILE_PARAM, TEST_ACCESS_KEY_ID),
      String.format("%s=%s", AWS_SECRET_KEY_CONFIG_FILE_PARAM, TEST_SECRET_ACCESS_KEY),
      String.format("%s=%s", AWS_SESSION_TOKEN_CONFIG_FILE_PARAM, TEST_SESSION_TOKEN),
      String.format("%s=%s", AWS_REGION_CONFIG_FILE_PARAM, TEST_AWS_REGION)
    );
    for (String expectedAwsCredentialsValue : expectedAwsCredentialsValues) {
      if (!decodedCredentials.contains(expectedAwsCredentialsValue)) {
        fail("Encoded credentials are incorrect: " + expectedAwsCredentialsValue + " value is not in the encoded credentials content");
        return false;
      }
    }

    return true;
  }
}