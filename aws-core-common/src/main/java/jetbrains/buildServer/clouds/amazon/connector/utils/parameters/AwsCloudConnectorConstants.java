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

  //XML
  public static final String AWS_CALLER_IDENTITY_ELEMENT = "callerIdentity";
  public static final String AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID = "accountId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ID = "userId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ARN = "userArn";

  //AVAIL AWS CONNs
  public static final String AVAIL_AWS_CONNECTIONS_CONTROLLER_URL = "/admin/oauth/availAwsConnections.html";

  public static final String AVAIL_AWS_CONNECTIONS_ELEMENT = "availableAwsConnections";
  public static final String AWS_CONNECTION_ELEMENT = "awsConnection";
  public static final String AWS_CONNECTION_ATTR_NAME = "name";
  public static final String AWS_CONNECTION_ATTR_ID = "id";
  public static final String AWS_CONNECTION_ATTR_DESCRIPTION = "description";
  public static final String AWS_CONNECTION_ATTR_OWN_PROJ_ID = "ownerProjectId";

  public static final String CHOSEN_AWS_CONN_ID_PARAM = "aws.chosen.connection.id";
}
