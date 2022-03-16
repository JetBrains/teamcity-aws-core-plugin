package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class ParamUtil {

  public static boolean useSessionCredentials(Map<String, String> properties){
    return "true".equals(properties.get(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM));
  }

  public static boolean isValidSessionDuration(@Nullable String strSessionDuration) {
    if (strSessionDuration == null) {
      return false;
    }
    if(isEmptyString(strSessionDuration)){
      return true;
    }
    try {
      int num = Integer.parseInt(strSessionDuration);
      if(num <= 0 || num < AwsAccessKeysParams.MIN_SESSION_DURATION || num > AwsAccessKeysParams.MAX_SESSION_DURATION)
        return false;
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static int getSesseionDurationMinutes(Map<String, String> cloudConnectorProperties) {
    String sessionDurationStr = cloudConnectorProperties.get(AwsAccessKeysParams.SESSION_DURATION_PARAM);
    if(isEmptyString(sessionDurationStr) || ! isValidSessionDuration(sessionDurationStr)) {
      return Integer.parseInt(AwsAccessKeysParams.SESSION_DURATION_DEFAULT);
    } else {
      return Integer.parseInt(sessionDurationStr);
    }
  }

  private static boolean isEmptyString(String string){
    return "".equals(string.replaceAll("\\s", ""));
  }
}
