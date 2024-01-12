

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

public final class AwsSessionCredentialsParams {
  public static final String SESSION_DURATION_PARAM = "awsSessionDuration";
  public static final String SESSION_DURATION_LABEL = "Session duration:";
  public static final String SESSION_DURATION_DEFAULT = "60";
  public static final int SESSION_DURATION_DEFAULT_NUMBER = 60;

  public static final int MIN_SESSION_DURATION = 15;
  public static final int MAX_SESSION_DURATION = 2160;
}