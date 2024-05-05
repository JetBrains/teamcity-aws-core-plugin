

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.AVAIL_AWS_CONNS_JSP_FILE_NAME;

public final class AwsAssumeIamRoleParams {

  public static final String IAM_ROLE_LABEL = "IAM Role";

  public static final String IAM_ROLE_ARN_PARAM = "awsIamRoleArn";
  public static final String IAM_ROLE_ARN_LABEL = "Role ARN:";
  public static final String IAM_ROLE_SESSION_NAME_PARAM = "awsIamRoleSessionName";
  public static final String IAM_ROLE_SESSION_NAME_LABEL = "Session tag:";
  public static final String IAM_ROLE_SESSION_NAME_DEFAULT = "TeamCity-session";

  public static final String STS_ENDPOINT_FIELD_ID_IAM_ROLE = "stsEndpointFieldIamRole";

  public static final String VALID_ROLE_SESSION_NAME_REGEX = "[\\w+=,.@%-]*";

  public static final String AVAIL_AWS_CONNS_URL = "../../availableAwsConnections/" + AVAIL_AWS_CONNS_JSP_FILE_NAME;

  public static final String EXTERNAL_IDS_CONTROLLER_URL = "/admin/connections/aws/externalIds.html";
  public static final String EXTERNAL_ID_FIELD_ID = "externalIdValue";
}