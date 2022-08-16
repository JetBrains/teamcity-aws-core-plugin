package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionIdGenerator.AWS_CONNECTION_ID_PREFIX;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.ACCESS_KEY_ID_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AwsConnectionIdGeneratorTest extends BaseTestCase {

  private AwsConnectionIdGenerator myAwsConnectionIdGenerator;
  private ArrayList<String> myAwsExistedConnectionIDx;

  private Map<String, String> myDataStorageValues;

  private ProjectManager projectManager;

  @BeforeMethod
  public void setup() throws Exception {
    super.setUp();

    myAwsExistedConnectionIDx = new ArrayList<>();
    myDataStorageValues = new HashMap<>();

    projectManager = Mockito.mock(ProjectManager.class, Answers.RETURNS_DEEP_STUBS);

    myAwsConnectionIdGenerator = new AwsConnectionIdGenerator(
      Mockito.mock(OAuthConnectionsIdGenerator.class),
      projectManager
    );

    initExistedIdx();
    initDataStorageIdx();

    CustomDataStorage customDataStorage = Mockito.mock(CustomDataStorage.class, Answers.RETURNS_DEEP_STUBS);

    when(projectManager.getRootProject().getCustomDataStorage(any()))
      .thenReturn(customDataStorage);

    when(customDataStorage.getValues())
      .thenReturn(myDataStorageValues);
  }

  @Test
  public void testAddingOfExistedIds() {
    for (String id : myAwsExistedConnectionIDx) {
      myAwsConnectionIdGenerator.addGeneratedId(id, Collections.emptyMap());
    }

    for (String id : myAwsExistedConnectionIDx) {
      assertFalse(
        "After adding existed ids, they shold be in the map",
        myAwsConnectionIdGenerator.isUnique(id)
      );
    }
  }

  @Test
  public void whenRootProjectIsNotInitialisedThenReturnRandomId() {

    when(projectManager.getRootProject().getCustomDataStorage(any()))
      .thenThrow(new Exception("Has not been initialised yet"));

    Pattern pattern = Pattern.compile(AWS_CONNECTION_ID_PREFIX + "*", Pattern.CASE_INSENSITIVE);

    myAwsConnectionIdGenerator.newId(createDefaultConnectionProps());

    Set<String> set = myAwsConnectionIdGenerator
      .getAwsConnectionIdx()
      .keySet()
      .stream()
      .filter(k -> pattern.matcher(k).matches())
      .collect(Collectors.toSet());

    assertEquals(1, set.size());
    assertFalse(set.contains(AWS_CONNECTION_ID_PREFIX + "0"));
  }


  private void initExistedIdx() {
    for (int i = 0; i < 3; i++) {
      myAwsExistedConnectionIDx.add(UUID.randomUUID().toString());
    }
  }

  private void initDataStorageIdx() {
    for (int i = 0; i < 2; i++) {
      String newId = UUID.randomUUID().toString();
      myDataStorageValues.put(newId, newId);
    }
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