package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ParamUtilTest {
  private final String TEST_RESOURCE_PATH = "/path/subResource";
  private final String TEST_RESOURCE_NAME = "resource-id" + TEST_RESOURCE_PATH;


  @Test
  public void testGetResourceNameFromArnWhenResorceIsAnyWithSlash() {
    String arn = "arn:partition:service:region:account-id:resource-type/" + TEST_RESOURCE_NAME;
    assertEquals(
      ParamUtil.getResourceNameFromArn(arn),
      TEST_RESOURCE_NAME
    );
  }

  @Test
  public void testGetResourceNameFromArnWhenResorceIsAnyWithoutSlash() {
    String arn = "arn:partition:service:region:account-id:resource-type:" + TEST_RESOURCE_NAME;
    assertEquals(
      ParamUtil.getResourceNameFromArn(arn),
      TEST_RESOURCE_NAME
    );
  }

  @Test
  public void testGetResourceNameFromArnWhenResorceWithoutResourceType() {
    //there is no way to know when the resource path is actually starts,
    //we can have any number of slashes at the end,
    //so we will cut the first part of the path in the case when there is no resource type :
    String expectedResultWhenNoResourceType = TEST_RESOURCE_PATH.substring(1);
    String arn = "arn:partition:service:region:account-id:" + TEST_RESOURCE_NAME;
    assertEquals(
      ParamUtil.getResourceNameFromArn(arn),
      expectedResultWhenNoResourceType
    );
  }

  @Test
  public void testGetResourceNameFromArnWhenResorceWithoutPath() {
    String resourceNameNoPath = "resource-id";
    String arn = "arn:partition:service:region:account-id:resource-type:" + resourceNameNoPath;
    assertEquals(
      ParamUtil.getResourceNameFromArn(arn),
      resourceNameNoPath
    );
  }

  @Test
  public void testGetResourceNameFromArnWhenResorceWithoutPathWithSlash() {
    String resourceNameNoPath = "resource-id";
    String arn = "arn:partition:service:region:account-id:resource-type/" + resourceNameNoPath;
    assertEquals(
      ParamUtil.getResourceNameFromArn(arn),
      resourceNameNoPath
    );
  }

  @Test
  public void testGetResourceNameFromArnWhenResorceIsIamRole() {
    String roleName = "role-name-with-path";
    String iamRoleArn = "arn:aws:iam::account:role/" + roleName;

    assertEquals(
      ParamUtil.getResourceNameFromArn(iamRoleArn),
      roleName
    );
  }
}