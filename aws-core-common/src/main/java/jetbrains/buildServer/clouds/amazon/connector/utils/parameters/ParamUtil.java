package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.Map;

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static com.intellij.openapi.util.text.StringUtil.parseInt;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;

public class ParamUtil {

  public static boolean useSessionCredentials(@NotNull final Map<String, String> properties){
    String useSessionCredentials = properties.get(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM);
    if(StringUtil.isEmptyOrSpaces(useSessionCredentials)){
      return true;
    }
    return "true".equals(useSessionCredentials);
  }

  public static String maskKey(String value) {
    if (value.length() > AwsAccessKeysParams.KEY_MASK_VISIBLE_SYMBOLS)
      return AwsAccessKeysParams.KEY_MASK + value.substring(value.length() - AwsAccessKeysParams.KEY_MASK_VISIBLE_SYMBOLS);
    else
      return AwsAccessKeysParams.KEY_MASK + value;
  }

  public static boolean isValidSessionDuration(@Nullable final String strSessionDuration) {
    if(strSessionDuration == null || isEmptyOrSpaces(strSessionDuration)){
      return true;
    }
    try {
      int sessionDurationNumber = parseInt(strSessionDuration, SESSION_DURATION_DEFAULT_NUMBER);
      if(sessionDurationNumber <= 0 || sessionDurationNumber < AwsSessionCredentialsParams.MIN_SESSION_DURATION || sessionDurationNumber > AwsSessionCredentialsParams.MAX_SESSION_DURATION)
        return false;
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static int getSessionDurationMinutes(@NotNull final Map<String, String> cloudConnectorProperties) {
    String sessionDurationStr = cloudConnectorProperties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM);
    if(sessionDurationStr == null || ! isValidSessionDuration(sessionDurationStr)) {
      return SESSION_DURATION_DEFAULT_NUMBER;
    } else {
      return parseInt(sessionDurationStr, SESSION_DURATION_DEFAULT_NUMBER);
    }
  }
}
