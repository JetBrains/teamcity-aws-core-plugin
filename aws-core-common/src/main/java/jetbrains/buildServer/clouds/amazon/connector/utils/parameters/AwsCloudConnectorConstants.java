package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public interface AwsCloudConnectorConstants {

  final String FEATURE_PROPERTY_NAME = "teamcity.internal.awsConnectorEnabled";

  final String CLOUD_TYPE = "AWS";

  public static final String DEFAULT_SUFFIX = "aws";
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  public static final String CREDENTIALS_TYPE_PARAM = "aws.credentials.type";

  public static final String STATIC_CREDENTIALS_TYPE = "aws.access.keys";
}
