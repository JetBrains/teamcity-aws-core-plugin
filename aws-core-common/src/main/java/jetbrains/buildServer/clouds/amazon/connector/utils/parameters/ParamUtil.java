package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import org.jetbrains.annotations.Nullable;

public class ParamUtil {
  public static boolean isValidSessionDuration(@Nullable String strSessionDuration) {
    if (strSessionDuration == null) {
      return false;
    }
    if("".equals(strSessionDuration.replaceAll("\\s", ""))){
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
}
