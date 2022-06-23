package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsCloudConnectorConstants {

  public static final String FEATURE_PROPERTY_NAME = "teamcity.internal.awsConnectorEnabled";

  public static final String CLOUD_TYPE = "AWS";

  public static final String DEFAULT_SUFFIX = "aws";
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  public static final String CREDENTIALS_TYPE_PARAM = "aws.credentials.type";
  public static final String CREDENTIALS_TYPE_LABEL = "Type";

  public static final String STATIC_CREDENTIALS_TYPE = "aws.access.keys";
  public static final String ACCESS_KEYS_LABEL = "Access keys";

  public static final String REGION_NAME_PARAM = "aws.region.name";
  public static final String REGION_NAME_LABEL = "AWS region";
  public static final String REGION_NAME_DEFAULT = "us-east-1";
  public static final String REGION_SELECT_ID = "regionSelect";

  public static final String STS_ENDPOINT_FIELD_ID = "stsEndpointField";


  //Test connection
  public static final String TEST_CONNECTION_CONTROLLER_URL = "/repo/aws-test-connection.html";

  public static final String AWS_CALLER_IDENTITY_ELEMENT = "callerIdentity";
  public static final String AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID = "accountId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ID = "userId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ARN = "userArn";

  //Available connections
  public static final String AVAIL_AWS_CONNECTIONS_CONTROLLER_URL = "/admin/oauth/availAwsConnections.html";
  public static final String AVAIL_AWS_CONNECTIONS_REST_RESOURCE_NAME = "availableAwsConnections";
  public static final String AVAIL_AWS_CONNS_JSP_FILE_NAME = "availableAwsConnections.jsp";
  public static final String AVAIL_AWS_CONNS_BUILD_FORM_JSP_FILE_NAME = "availAwsConnsForBuildForm.jsp";
  public static final String AVAIL_AWS_CONNS_PROJ_FEATURE_JSP_FILE_NAME = "availAwsConnsForProjectFeature.jsp";
  public static final String SESSION_CREDS_CONFIG_JSP_FILE_NAME = "sessionCredentialsConfig.jsp";

  public static final String PRINCIPAL_AWS_CONNECTION_ID = "principalAwsConnId";
  public static final String AVAIL_AWS_CONNECTIONS_SELECT_ID = "availableAwsConnectionsSelect";
  public static final String CHOSEN_AWS_CONN_ID_PARAM = "aws.connection.id";
  public static final String CHOSEN_AWS_CONN_ID_LABEL = "AWS Connection";
}
