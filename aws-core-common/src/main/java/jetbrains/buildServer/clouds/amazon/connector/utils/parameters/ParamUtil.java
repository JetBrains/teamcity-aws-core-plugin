package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.VALID_ROLE_SESSION_NAME_REGEX;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;

public class ParamUtil {

  public static boolean useSessionCredentials(@NotNull final Map<String, String> properties){
    String useSessionCredentials = properties.get(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM);
    if("false".equals(useSessionCredentials))
      return false;

    return true;
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
      if(sessionDurationNumber < AwsSessionCredentialsParams.MIN_SESSION_DURATION || sessionDurationNumber > AwsSessionCredentialsParams.MAX_SESSION_DURATION)
        return false;
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static boolean isValidSessionName(@Nullable final String sessionName) {
    if(sessionName == null)
      return false;
    Pattern validAwsSessionNamePattern = Pattern.compile(VALID_ROLE_SESSION_NAME_REGEX);
    return validAwsSessionNamePattern.matcher(sessionName).matches();
  }

  public static int getSessionDurationMinutes(@NotNull final Map<String, String> cloudConnectorProperties) {
    String sessionDurationStr = cloudConnectorProperties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM);
    if(sessionDurationStr == null || ! isValidSessionDuration(sessionDurationStr)) {
      return SESSION_DURATION_DEFAULT_NUMBER;
    } else {
      return parseInt(sessionDurationStr, SESSION_DURATION_DEFAULT_NUMBER);
    }
  }

  /**
   * Extract the part after "/" symbol.
   * @param  resourceArn  ARN of the resource where to find the name.
   * @return Empty String if ARN is empty or the part after "/" symbol otherwise.
   */
  @NotNull
  public static String getResourceNameFromArn(@Nullable final String resourceArn) {
    if(isEmptyOrSpaces(resourceArn)){
      return "";
    }
    List<String> parts = split(resourceArn, "/");
    if (parts.size() > 1){
      return parts.get(1);
    }
    return "";
  }
}
