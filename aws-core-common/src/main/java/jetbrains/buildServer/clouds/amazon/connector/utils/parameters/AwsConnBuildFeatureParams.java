package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsConnBuildFeatureParams {
  public static final String AWS_CONN_TO_ENV_VARS_BUILD_FEATURE_TYPE = "AWS_CREDS_TO_AGENT";

  public static final String AWS_ACCESS_KEY_ENV_PARAM_DEFAULT = "aws_access_key_id";
  public static final String AWS_SECRET_KEY_ENV_PARAM_DEFAULT = "aws_secret_access_key";
  public static final String AWS_SESSION_TOKEN_ENV_PARAM_DEFAULT = "aws_session_token";

  public static final String AWS_REGION_ENV_PARAM_DEFAULT = "AWS_DEFAULT_REGION";

  public static final String AWS_SHARED_CREDENTIALS_FILE = "AWS_SHARED_CREDENTIALS_FILE";
  public static final String AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT = "__internal_aws_credentials";
}
