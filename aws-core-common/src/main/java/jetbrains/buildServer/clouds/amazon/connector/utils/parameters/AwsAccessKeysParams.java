package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsAccessKeysParams {
  public static final String ACCESS_KEY_ID_PARAM = "aws.access.key.id";

  public static final String SECURE_SECRET_ACCESS_KEY_PARAM = "secure:aws.secret.access.key";

  public static final String SESSION_CREDENTIALS_PARAM = "aws.session.credentials";

  public static final String SESSION_DURATION_PARAM = "aws.session.duration";

  public static final String STS_ENDPOINT_PARAM = "aws.sts.endpoint";
  public static final String STS_ENDPOINT_DEFAULT = "https://sts.amazonaws.com";


  public static final int MIN_SESSION_DURATION = 900;
  public static final int MAX_SESSION_DURATION = 129600;
}
