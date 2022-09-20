package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator.AWS_CONNECTION_ID_PREFIX;
import static jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator.INITIAL_CURRENT_AWS_CONNECTION_ID;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.ACCESS_KEY_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;

public class AwsConnectionIdGeneratorTest extends BaseTestCase {

  private final String USER_DEFINED_AWS_CONN_ID = "MY_OWN_CONNECTION_ID";
  private AwsConnectionIdGenerator myAwsConnectionIdGenerator;
  private ArrayList<String> myAwsExistedConnectionIDx;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myAwsExistedConnectionIDx = new ArrayList<>();

    myAwsConnectionIdGenerator = new AwsConnectionIdGenerator(
      Mockito.mock(OAuthConnectionsIdGenerator.class)
    );

    initExistedIdx();
  }

  @Test
  public void whenAddedExistinIdsTheyShouldBeInTheMap() {
    for (String id : myAwsExistedConnectionIDx) {
      assertFalse(
        "After adding existed ids, they shold be in the map",
        myAwsConnectionIdGenerator.isUnique(id)
      );
    }
  }

  @Test
  public void whenUserDefinedConnectionIdThenUseDefinedId() {
    Map<String, String> awsConnProps = createDefaultConnectionProps();
    awsConnProps.put(USER_DEFINED_ID_PARAM, USER_DEFINED_AWS_CONN_ID);

    myAwsConnectionIdGenerator.createNextId(awsConnProps);

    assertEquals(
      USER_DEFINED_AWS_CONN_ID,
      myAwsConnectionIdGenerator.getAwsConnectionIdx()
                                .get(USER_DEFINED_AWS_CONN_ID)
    );
  }

  @Test
  public void whenUserDidNotSpecifiedConnectionIdThenUseCurrentIncrementalId() {

    myAwsConnectionIdGenerator.createNextId(createDefaultConnectionProps());
    String resultConnectionId = buildAwsConnId(INITIAL_CURRENT_AWS_CONNECTION_ID + 1);

    assertEquals(
      resultConnectionId,
      myAwsConnectionIdGenerator.getAwsConnectionIdx()
                                .get(resultConnectionId)
    );
  }

  @Test
  public void whenThereAre10InitialAwsConnsThenUseCorrectIncrementalId() {

    addIds(10);

    myAwsConnectionIdGenerator.createNextId(createDefaultConnectionProps());
    String resultConnectionId = buildAwsConnId(11);

    assertEquals(
      resultConnectionId,
      myAwsConnectionIdGenerator.getAwsConnectionIdx()
                                .get(resultConnectionId)
    );
  }

  private void initExistedIdx() {
    for (int i = 0; i < 3; i++) {
      myAwsExistedConnectionIDx.add(UUID.randomUUID().toString());
    }
    for (String id : myAwsExistedConnectionIDx) {
      myAwsConnectionIdGenerator.addGeneratedId(id, Collections.emptyMap());
    }
  }

  private void addIds(int quantity) {
    for (int i = 1; i <= quantity; i++) {
      myAwsConnectionIdGenerator.addGeneratedId(buildAwsConnId(i), Collections.emptyMap());
    }
  }

  private String buildAwsConnId(int number) {
    return AWS_CONNECTION_ID_PREFIX + "-" + String.valueOf(number);
  }

  private Map<String, String> createDefaultConnectionProps() {
    Map<String, String> res = new HashMap<>();
    res.put(ACCESS_KEY_ID_PARAM, "");
    res.put(SECURE_SECRET_ACCESS_KEY_PARAM, "");
    res.put(CREDENTIALS_TYPE_PARAM, STATIC_CREDENTIALS_TYPE);
    res.put(REGION_NAME_PARAM, REGION_NAME_DEFAULT);
    return res;
  }
}