package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsConnBuildFeatureParams {
  public static final String AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE = "PROVIDE_AWS_CREDS";

  public static final String AWS_ACCESS_KEY_CONFIG_FILE_PARAM = "aws_access_key_id";
  public static final String AWS_SECRET_KEY_CONFIG_FILE_PARAM = "aws_secret_access_key";
  public static final String AWS_SESSION_TOKEN_CONFIG_FILE_PARAM = "aws_session_token";

  public static final String AWS_REGION_CONFIG_FILE_PARAM = "region";

  public static final String AWS_SHARED_CREDENTIALS_FILE_ENV = "AWS_SHARED_CREDENTIALS_FILE";
  public static final String AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT = "__internal_aws_credentials";

  public static final String AWS_PROFILE_NAME_PARAM = "awsProfile";
  public static final String AWS_PROFILE_NAME_LABEL = "AWS Profile Name";
  public static final String AWS_PROFILE_NAME_ENV = "AWS_PROFILE";
  public static final String AWS_PROFILE_NAME_REGEXP = "^[^\\s]+$";
}
