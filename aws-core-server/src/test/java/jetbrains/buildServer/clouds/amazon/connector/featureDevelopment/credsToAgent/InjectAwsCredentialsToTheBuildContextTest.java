package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import com.intellij.util.containers.hash.HashMap;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.LinkedAwsConnectionProviderImpl;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import jetbrains.buildServer.serverSide.connections.ProjectConnectionsManager;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import jetbrains.buildServer.serverSide.impl.BuildFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.impl.NoOpBuildLog;
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
import static org.testng.Assert.*;

public class InjectAwsCredentialsToTheBuildContextTest {
  private final String TEST_AWS_CONNECTION_ID = "TEST_AWS_CONNECTION_ID";
  private final String TEST_ACCESS_KEY_ID = "TEST_ACCESS_KEY_ID";
  private final String TEST_SECRET_ACCESS_KEY = "TEST_SECRET_ACCESS_KEY";
  private final String TEST_SESSION_TOKEN = "TEST_SESSION_TOKEN";
  private final String TEST_AWS_REGION = "eu-west-1";

  private final String TEST_AWS_CONNECTION_ID_2 = "TEST_AWS_CONNECTION_ID_2";
  private final String TEST_ACCESS_KEY_ID_2 = "TEST_ACCESS_KEY_ID_2";
  private final String TEST_SECRET_ACCESS_KEY_2 = "TEST_SECRET_ACCESS_KEY_2";
  private final String TEST_SESSION_TOKEN_2 = "TEST_SESSION_TOKEN_2";
  private final String TEST_AWS_REGION_2 = "eu-west-2";
  private final String TEST_AWS_PROFILE = "test-profile";

  private InjectAwsCredentialsToTheBuildContext injectAwsCredentials;

  private BuildStartContext mockedBuildStartContext;
  private ExtensionHolder mockedExtensionHolder;
  private SRunningBuild runningBuild;
  private BuildTypeEx buildType;
  private ProjectConnectionsManager mockedConnectionsManager;
  private ProjectConnectionCredentialsManager mockedConnectionCredentialsManager;

  private Collection<SBuildFeatureDescriptor> awsCredentialsBuildFeatures;

  @BeforeMethod
  public void setup() throws Exception {
    mockedExtensionHolder = Mockito.mock(ExtensionHolder.class);
    ProjectManager mockedProjectManager = Mockito.mock(ProjectManager.class, RETURNS_DEEP_STUBS);
    mockedConnectionsManager = Mockito.mock(ProjectConnectionsManager.class, RETURNS_DEEP_STUBS);
    mockedConnectionCredentialsManager = Mockito.mock(ProjectConnectionCredentialsManager.class, RETURNS_DEEP_STUBS);

    LinkedAwsConnectionProvider linkedAwsConnectionProvider = new LinkedAwsConnectionProviderImpl(
      mockedProjectManager,
      mockedConnectionsManager,
      mockedConnectionCredentialsManager
    );
    injectAwsCredentials = new InjectAwsCredentialsToTheBuildContext(linkedAwsConnectionProvider);

    mockedBuildStartContext = Mockito.mock(BuildStartContext.class, RETURNS_DEEP_STUBS);
    runningBuild = Mockito.mock(SRunningBuild.class, RETURNS_DEEP_STUBS);
    buildType = Mockito.mock(BuildTypeEx.class);
    when(mockedBuildStartContext.getBuild()).thenReturn(runningBuild);
    when(runningBuild.getBuildType()).thenReturn(buildType);
    when(runningBuild.getBuildLog()).thenReturn(new NoOpBuildLog());

    awsCredentialsBuildFeatures = new ArrayList<>();

    when(runningBuild.getBuildFeaturesOfType(AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE))
      .thenReturn(awsCredentialsBuildFeatures);
  }

  private void givenAwsConnectionAndBuildFeature(
    String connectionId,
    String awsCredentialsBuildFeatureId,
    String awsProfileName,
    String accessKeyId,
    String secretAccessKey,
    String sessionToken,
    String awsRegion
  ) {
    Map<String, String> buildFeatureProperties = new HashMap<>();
    buildFeatureProperties.put(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM, connectionId);
    if (awsProfileName != null) {
      buildFeatureProperties.put(AWS_PROFILE_NAME_PARAM, awsProfileName);
    }

    awsCredentialsBuildFeatures.add(
      new BuildFeatureDescriptorImpl(
        awsCredentialsBuildFeatureId,
        AwsConnBuildFeatureParams.AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE,
        buildFeatureProperties,
        mockedExtensionHolder
      )
    );
    ConnectionDescriptor mockedConnectionDescriptor = createConnectionDescriptor(
      "PROJECT_ID",
      connectionId,
      Collections.emptyMap()
    );
    when(mockedConnectionsManager.findConnectionById(any(), eq(connectionId)))
      .thenReturn(mockedConnectionDescriptor);

    try {
      when(mockedConnectionCredentialsManager.requestConnectionCredentials(any(), eq(connectionId), any()))
        .thenReturn(
          new ConnectionCredentials() {
            @NotNull
            @Override
            public Map<String, String> getProperties() {
              Map<String, String> creds = new HashMap<>();
              creds.put(ACCESS_KEY_ID, accessKeyId);
              creds.put(SECRET_ACCESS_KEY, secretAccessKey);
              creds.put(SESSION_TOKEN, sessionToken);
              creds.put(REGION, awsRegion);
              return creds;
            }

            @NotNull
            @Override
            public String getProviderType() {
              return AwsConnectionProvider.TYPE;
            }
          }
        );
    } catch (ConnectionCredentialsException e) {
      fail("Unexpected exception", e);
    }
  }

  @Test
  void given_aws_conn_with_credentials_then_inject_correct_file_contents() {
    givenAwsConnectionAndBuildFeature(
      TEST_AWS_CONNECTION_ID,
      "INJECT_AWS_CONN_TO_BUILD_ID",
      TEST_AWS_PROFILE,
      TEST_ACCESS_KEY_ID,
      TEST_SECRET_ACCESS_KEY,
      TEST_SESSION_TOKEN,
      TEST_AWS_REGION
    );

    ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
    doAnswer(invocation -> {
      if (AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT.equals(arg1.getValue())) {
        assertTrue(credentialsCorrectlyEncoded(arg2.getValue()));
      }
      return null;
    }).when(mockedBuildStartContext).addSharedParameter(arg1.capture(), arg2.capture());

    injectAwsCredentials.updateParameters(mockedBuildStartContext);

    verify(mockedBuildStartContext).addSharedParameter(INJECTED_AWS_ACCESS_KEYS, TEST_ACCESS_KEY_ID);
    verify(mockedBuildStartContext).addSharedParameter(AWS_PROFILE_NAME_PARAM, TEST_AWS_PROFILE);
  }

  @Test
  void given_multiple_aws_conns_with_credentials_then_inject_correct_file_contents() {
    givenAwsConnectionAndBuildFeature(
      TEST_AWS_CONNECTION_ID,
      "INJECT_AWS_CONN_TO_BUILD_ID",
      null,
      TEST_ACCESS_KEY_ID,
      TEST_SECRET_ACCESS_KEY,
      TEST_SESSION_TOKEN,
      TEST_AWS_REGION
    );
    givenAwsConnectionAndBuildFeature(
      TEST_AWS_CONNECTION_ID_2,
      "INJECT_AWS_CONN_TO_BUILD_ID_2",
      TEST_AWS_PROFILE,
      TEST_ACCESS_KEY_ID_2,
      TEST_SECRET_ACCESS_KEY_2,
      TEST_SESSION_TOKEN_2,
      TEST_AWS_REGION_2
    );

    Map<String, String> sharedParameters = new ConcurrentHashMap<>();
    AtomicInteger invocationCount = new AtomicInteger();

    when(mockedBuildStartContext.getSharedParameters())
      .thenReturn(sharedParameters);

    ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
    doAnswer(invocation -> {
      if (AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT.equals(arg1.getValue())) {
        if (invocationCount.get() > 0) {
          multipleCredentialsCorrectlyEncoded(arg2.getValue());
        } else {
          sharedParameters.put(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, arg2.getValue());
          invocationCount.getAndIncrement();
        }
      }
      return null;
    }).when(mockedBuildStartContext).addSharedParameter(arg1.capture(), arg2.capture());

    injectAwsCredentials.updateParameters(mockedBuildStartContext);
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

  private void multipleCredentialsCorrectlyEncoded(String actualEncodedCredentials) {
    byte[] decodedBytes = Base64.getDecoder().decode(actualEncodedCredentials);
    String decodedCredentials = new String(decodedBytes, StandardCharsets.UTF_8);

    assertTrue(decodedCredentials.contains("[default]"));
    assertTrue(decodedCredentials.contains(AWS_ACCESS_KEY_CONFIG_FILE_PARAM + "=" + TEST_ACCESS_KEY_ID + "\n"));
    assertTrue(decodedCredentials.contains(AWS_SECRET_KEY_CONFIG_FILE_PARAM + "=" + TEST_SECRET_ACCESS_KEY + "\n"));
    assertTrue(decodedCredentials.contains(AWS_SESSION_TOKEN_CONFIG_FILE_PARAM + "=" + TEST_SESSION_TOKEN + "\n"));
    assertTrue(decodedCredentials.contains(AWS_REGION_CONFIG_FILE_PARAM + "=" + TEST_AWS_REGION + "\n"));

    assertTrue(decodedCredentials.contains("[test-profile]"));
    assertTrue(decodedCredentials.contains(AWS_ACCESS_KEY_CONFIG_FILE_PARAM + "=" + TEST_ACCESS_KEY_ID_2 + "\n"));
    assertTrue(decodedCredentials.contains(AWS_SECRET_KEY_CONFIG_FILE_PARAM + "=" + TEST_SECRET_ACCESS_KEY_2 + "\n"));
    assertTrue(decodedCredentials.contains(AWS_SESSION_TOKEN_CONFIG_FILE_PARAM + "=" + TEST_SESSION_TOKEN_2 + "\n"));
    assertTrue(decodedCredentials.contains(AWS_REGION_CONFIG_FILE_PARAM + "=" + TEST_AWS_REGION_2 + "\n"));
  }
}