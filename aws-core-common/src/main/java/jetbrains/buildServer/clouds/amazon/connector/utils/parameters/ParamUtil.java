package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static com.intellij.openapi.util.text.StringUtil.parseInt;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.SESSION_DURATION_DEFAULT_NUMBER;

public class ParamUtil {

  public static boolean useSessionCredentials(@NotNull final Map<String, String> properties){
    return "true".equals(properties.get(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM));
  }

  public static boolean isValidSessionDuration(@Nullable final String strSessionDuration) {
    if(strSessionDuration == null || isEmptyOrSpaces(strSessionDuration)){
      return true;
    }
    try {
      int sessionDurationNumber = parseInt(strSessionDuration, SESSION_DURATION_DEFAULT_NUMBER);
      if(sessionDurationNumber <= 0 || sessionDurationNumber < AwsAccessKeysParams.MIN_SESSION_DURATION || sessionDurationNumber > AwsAccessKeysParams.MAX_SESSION_DURATION)
        return false;
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static int getSessionDurationMinutes(@NotNull final Map<String, String> cloudConnectorProperties) {
    String sessionDurationStr = cloudConnectorProperties.get(AwsAccessKeysParams.SESSION_DURATION_PARAM);
    if(! isValidSessionDuration(sessionDurationStr)) {
      return SESSION_DURATION_DEFAULT_NUMBER;
    } else {
      return Integer.parseInt(sessionDurationStr);
    }
  }
}
