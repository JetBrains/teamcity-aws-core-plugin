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

  public static final String TEST_CONNECTION_CONTROLLER_URL = "/repo/aws-test-connection.html";

  //XML
  public static final String AWS_CALLER_IDENTITY_ELEMENT = "callerIdentity";
  public static final String AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID = "accountId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ID = "userId";
  public static final String AWS_CALLER_IDENTITY_ATTR_USER_ARN = "userArn";
}
