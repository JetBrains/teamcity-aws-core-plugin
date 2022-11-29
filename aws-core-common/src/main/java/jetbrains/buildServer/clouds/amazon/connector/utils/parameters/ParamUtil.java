package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import com.amazonaws.arn.Arn;
import java.util.Map;

import java.util.regex.Pattern;

import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.VALID_ROLE_SESSION_NAME_REGEX;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;

public class ParamUtil {

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
      int sessionDurationNumber = Integer.parseInt(strSessionDuration);
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

  @Nullable
  public static String getInvalidArnReason(@Nullable final String resourceArnString) {
    if(isEmptyOrSpaces(resourceArnString)){
      return "ARN is empty";
    }
    try {
      Arn.fromString(resourceArnString);
      return null;
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }
  }

  /**
   * Extract the <b>resource-id</b> part of the ARN. <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">More info.</a>
   * @param  resourceArnString  ARN of the resource from where to extract the resource-id part.
   * @return Empty String if ARN is empty, resource-id or the ARN itself if it is malformed.
   */
  @NotNull
  public static String getResourceNameFromArn(@Nullable final String resourceArnString) {
    if(isEmptyOrSpaces(resourceArnString)){
      return "";
    }
    try {
      Arn resourceArn = Arn.fromString(resourceArnString);
      return resourceArn.getResource().getResource();
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  public static boolean isAwsConnectionFeature(@Nullable final SProjectFeatureDescriptor projectFeature) {
    if (projectFeature == null || !OAuthConstants.FEATURE_TYPE.equals(projectFeature.getType())) {
      return false;
    }
    String providerType = projectFeature.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
    return providerType != null && providerType.equals(AwsCloudConnectorConstants.CLOUD_TYPE);
  }
}
