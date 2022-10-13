package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.testUtils.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator.AWS_CONNECTION_ID_PREFIX;
import static jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator.INITIAL_AWS_CONNECTION_ID;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AwsConnectionIdGeneratorTest extends AwsConnectionTester {
  private static final String SOME_PROJECT_ID = "someProjectId";
  private final String USER_DEFINED_AWS_CONN_ID = "MY_OWN_CONNECTION_ID";
  private final String TEST_INITIAL_AWS_CONN_ID_1 = "MY_INITIAL_CONNECTION_ID_1";
  private final String TEST_INITIAL_AWS_CONN_ID_2 = "MY_INITIAL_CONNECTION_ID_2";
  private AwsConnectionIdGenerator myAwsConnectionIdGenerator;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myAwsConnectionIdGenerator = new AwsConnectionIdGenerator(
      getAwsConnectionsHolder(),
      Mockito.mock(OAuthConnectionsIdGenerator.class)
    );

    initExistedIdx();
  }

  @Test
  public void whenAddedExistinIdsTheyShouldBeInTheMap() {
    myDataStorageValues.forEach((connectionId, v) -> {
      assertFalse(
        "After adding existed ids, they shold be in the map",
        myAwsConnectionIdGenerator.isUnique(connectionId)
      );
    });
  }

  @Test
  public void whenUserDefinedConnectionIdThenUseThisId() {
    Map<String, String> awsConnProps = createConnectionDefaultProperties();
    awsConnProps.put(USER_DEFINED_ID_PARAM, USER_DEFINED_AWS_CONN_ID);

    String newId = myAwsConnectionIdGenerator.newId(awsConnProps);

    assertEquals(USER_DEFINED_AWS_CONN_ID, newId);
  }

  @Test
  public void whenUserDidNotSpecifiedConnectionIdThenUseIncrementalId() {

    String newId = myAwsConnectionIdGenerator.newId(createConnectionDefaultProperties());
    String resultConnectionId = AwsConnectionIdGenerator.formatId(AWS_CONNECTION_ID_PREFIX, INITIAL_AWS_CONNECTION_ID + 1);

    assertEquals(resultConnectionId, newId);
  }

  @Test
  public void whenThereAre10InitialAwsConnsThenUseCorrectIncrementalId() {
    for (int i = 0; i < 10; i++) {
      myDataStorageValues.put(myAwsConnectionIdGenerator.newId(createConnectionDefaultProperties()), SOME_PROJECT_ID);
    }

    String newId = myAwsConnectionIdGenerator.newId(createConnectionDefaultProperties());
    String resultConnectionId = AwsConnectionIdGenerator.formatId(AWS_CONNECTION_ID_PREFIX, 11);

    assertEquals(resultConnectionId, newId);
  }

  @Test
  public void whenAdding3ConnectionsThenUseCorrectIncrementalIds() {

    AwsCredentialsHolder someCredsHolder = new StaticCredentialsHolder("", "");
    AwsCredentialsBuilder someBuilder = Mockito.mock(AwsCredentialsBuilder.class);
    when(someBuilder.getCredentialsType())
      .thenReturn(AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    try {
      when(someBuilder.constructSpecificCredentialsProvider(any()))
        .thenReturn(someCredsHolder);
    } catch (AwsConnectorException e) {
      fail("Test failed with excption: " + e.getMessage());
    }

    getAwsConnectorFactory().registerAwsCredentialsBuilder(someBuilder);

    for (int i = 0; i < 3; i++) {
      ProjectFeatureDescriptorImpl newAwsConnectionFeature = new ProjectFeatureDescriptorImpl(
        myAwsConnectionIdGenerator.newId(createConnectionDefaultProperties()),
        OAuthConstants.FEATURE_TYPE,
        createConnectionDefaultProperties(),
        myProjectManager.getRootProject().getProjectId()
      );

      addTeamCityAwsConnection(
        myProjectManager.getRootProject(),
        newAwsConnectionFeature
      );

      getAwsConnectionsEventsListener().projectFeatureAdded(myProjectManager.getRootProject(), newAwsConnectionFeature);
    }

    for (int i = 1; i < 4; i++) {
      try {
        String id = AwsConnectionIdGenerator.formatId(AWS_CONNECTION_ID_PREFIX, i);
        AwsConnectionDescriptor awsConnection = getAwsConnectionsManager().getAwsConnection(id);
        assertEquals(id, awsConnection.getId());
      } catch (AwsConnectorException e) {
        fail("Test failed with excption: " + e.getMessage());
      }
    }
  }

  private void initExistedIdx() {
    myAwsConnectionIdGenerator.addGeneratedId(TEST_INITIAL_AWS_CONN_ID_1, Collections.emptyMap());
    myAwsConnectionIdGenerator.addGeneratedId(TEST_INITIAL_AWS_CONN_ID_2, Collections.emptyMap());
  }

  @Override
  protected Map<String, String> createDefaultStorageValues() {
    Map<String, String> res = new HashMap<>();
    res.put(TEST_INITIAL_AWS_CONN_ID_1, SOME_PROJECT_ID);
    res.put(TEST_INITIAL_AWS_CONN_ID_2, SOME_PROJECT_ID);
    return res;
  }
}