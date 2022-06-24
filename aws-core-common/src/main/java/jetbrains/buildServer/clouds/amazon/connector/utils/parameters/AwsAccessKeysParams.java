package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsAccessKeysParams {
  public static final String ACCESS_KEY_ID_PARAM = "awsAccessKeyId";
  public static final String ACCESS_KEY_ID_LABEL = "Access Key ID";


  public static final String SECURE_SECRET_ACCESS_KEY_PARAM = "secure:awsSecretAccessKey";
  public static final String SECRET_ACCESS_KEY_PARAM = "awsSecretAccessKey";
  public static final String SECRET_ACCESS_KEY_LABEL = "Secret Access Key";
  public static final String ROTATE_KEY_CONTROLLER_URL = "/aws-rotate-keys.html";
  public static final String ROTATE_KEY_BTTN_ID = "rotateKeyButton";
  public static final String AWS_CONN_DIALOG_NAME = "Amazon Web Services";

  public static final String SESSION_CREDENTIALS_PARAM = "awsSessionCredentials";
  public static final String SESSION_CREDENTIALS_LABEL = "Use session credentials:";
  public static final String SESSION_CREDENTIALS_DEFAULT = "true";


  public static final String STS_ENDPOINT_PARAM = "awsStsEndpoint";
  public static final String STS_ENDPOINT_LABEL = "STS endpoint:";
  public static final String STS_GLOBAL_ENDPOINT = "https://sts.amazonaws.com";

  public static final String KEY_MASK = "************";
  public static final byte KEY_MASK_VISIBLE_SYMBOLS = 4;

  //errors
  public static final String ACCESS_KEY_ID_ERROR = "Please provide the access key ID";
  public static final String SECRET_ACCESS_KEY_ERROR = "Please provide the secret access key ";
  public static final String REGION_ERROR = "Please choose the region where this AWS Connection will be used";
  public static final String SESSION_DURATION_ERROR = "Session duration is not valid";
}
