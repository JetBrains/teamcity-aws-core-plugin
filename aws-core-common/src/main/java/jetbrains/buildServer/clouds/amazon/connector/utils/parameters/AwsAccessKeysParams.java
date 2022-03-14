package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsAccessKeysParams {
  public static final String ACCESS_KEY_ID_PARAM = "aws.access.key.id";
  public static final String ACCESS_KEY_ID_LABEL = "Access Key ID";


  public static final String SECURE_SECRET_ACCESS_KEY_PARAM = "secure:aws.secret.access.key";
  public static final String SECRET_ACCESS_KEY_PARAM = "aws.secret.access.key";
  public static final String SECRET_ACCESS_KEY_LABEL = "Secret Access Key";
  public static final String ROTATE_KEY_CONTROLLER_URL = "/repo/aws-rotate-keys.html";
  public static final String ROTATE_KEY_BTTN_ID = "rotateKeyButton";

  public static final String KEY_ROTATION_PARAM = "aws.key.rotation";
  public static final String KEY_ROTATION_LABEL = "Rotate key periodically:";

  public static final String KEY_ROTATION_PERIOD_PARAM = "aws.key.rotation";
  public static final String KEY_ROTATION_PERIOD_LABEL = "Rotation period";
  public static final String KEY_ROTATION_WEEKLY_OPTION = "weekly";


  public static final String SESSION_CREDENTIALS_PARAM = "aws.session.credentials";
  public static final String SESSION_CREDENTIALS_LABEL = "Use session credentials:";
  public static final String SESSION_CREDENTIALS_DEFAULT = "true";

  public static final String SESSION_DURATION_PARAM = "aws.session.duration";
  public static final String SESSION_DURATION_LABEL = "Session duration:";
  public static final String SESSION_DURATION_DEFAULT = "3600";

  public static final int MIN_SESSION_DURATION = 900;
  public static final int MAX_SESSION_DURATION = 129600;


  public static final String STS_ENDPOINT_PARAM = "aws.sts.endpoint";
  public static final String STS_ENDPOINT_LABEL = "STS endpoint";
  public static final String STS_ENDPOINT_DEFAULT = "https://sts.amazonaws.com";
}
