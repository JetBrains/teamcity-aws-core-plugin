package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.VALID_ROLE_SESSION_NAME_REGEX;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;

public class ParamUtil {

  private final static int MIN_SEMICOLONS_QUANTITY_IN_AWS_ARN = 5;
  private final static String AWS_ARN_SEPARATOR = ":";
  private final static String AWS_ARN_RESOURCE_SEPARATOR = "/";

  private final static Pattern validAwsSessionNamePattern = Pattern.compile(VALID_ROLE_SESSION_NAME_REGEX);

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
   * Extract the <b>resource-id</b> part of the ARN. <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">More info.</a>
   * @param  resourceArn  ARN of the resource from where to extract the resource-id part.
   * @return Empty String if ARN is empty, resource-id or the ARN itself if it is malformed.
   */
  @NotNull
  public static String getResourceNameFromArn(@Nullable final String resourceArn) {
    if(isEmptyOrSpaces(resourceArn)){
      return "";
    }

    int semicilonsQuantity = StringUtils.countMatches(resourceArn, AWS_ARN_SEPARATOR);
    if (semicilonsQuantity > MIN_SEMICOLONS_QUANTITY_IN_AWS_ARN) {
      return getLastArnPart(resourceArn);

    } else if (resourceArn.contains(AWS_ARN_RESOURCE_SEPARATOR)) {
      return resourceArn.substring(resourceArn.indexOf(AWS_ARN_RESOURCE_SEPARATOR) + 1);
    }
    return getLastArnPart(resourceArn);
  }
  @NotNull
  private static String getLastArnPart(@NotNull String resourceArn) {
    List<String> parts = split(resourceArn, AWS_ARN_SEPARATOR);
    if (parts.size() > 0) {
      return parts.get(parts.size() - 1);
    }
    return resourceArn;
  }
}
