package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.Map;

import java.util.regex.Pattern;

import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;

import static com.intellij.openapi.util.text.StringUtil.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams.VALID_ROLE_SESSION_NAME_REGEX;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams.AWS_PROFILE_NAME_REGEXP;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams.*;

public class ParamUtil {

  private final static Pattern validAwsSessionNamePattern = Pattern.compile(VALID_ROLE_SESSION_NAME_REGEX);

  public static boolean useSessionCredentials(@NotNull final Map<String, String> properties){
    String useSessionCredentials = properties.getOrDefault(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM, AwsAccessKeysParams.SESSION_CREDENTIALS_DEFAULT);
    return !"false".equals(useSessionCredentials);
  }

  public static boolean isAllowedInSubProjects(@NotNull final Map<String, String> properties){
    String value = properties.get(AwsCloudConnectorConstants.ALLOWED_IN_SUBPROJECTS_PARAM);
    return value == null || Boolean.parseBoolean(value);
  }

  public static boolean toBooleanOrTrue(SBuildType buildType, String featureFlag) {
    BuildTypeEx buildTypeEx = (BuildTypeEx) buildType;
    return buildTypeEx.getBooleanInternalParameterOrTrue(featureFlag);
  }

  public static String getAwsProfileNameOrDefault(@Nullable final String awsProfileName) {
    return awsProfileName != null ? awsProfileName : "default";
  }

  public static boolean isValidAwsProfileName(@Nullable final String awsProfileName) {
    if(awsProfileName == null){
      return true;
    }
    return awsProfileName.matches(AWS_PROFILE_NAME_REGEXP);
  }

  @Deprecated
  /**
   * Deprecated, decided to stop masking AWS Access Key ID, it should be completely visible, the Secret Access Key is not visible.
   */
  public static String maskKey(String value) {
    return value;//TW-82810 TeamCity hides an AWS access key id as a secret
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
      ArnResource resource = resourceArn.resource();
      String resourceId = resource.resource();
      return resource.qualifier()
        .map(qual -> resourceId + "/" + qual)
        .orElse(resourceId);
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

  public static boolean isDefaultCredsProviderType(@NotNull final Map<String, String> connectionProperties) {
    return AwsCloudConnectorConstants.DEFAULT_PROVIDER_CREDENTIALS_TYPE.equals(connectionProperties.get(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM));
  }

  public static boolean isDefaultCredsProvidertypeDisabled() {
    return !TeamCityProperties.getBoolean(DEFAULT_CREDS_PROVIDER_FEATURE_PROPERTY_NAME);
  }

  @Nullable
  public static String getLinkedAwsConnectionId(@NotNull final Map<String, String> otherFeatureProperties) {
    return otherFeatureProperties.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM);
  }

  public static boolean withAwsConnectionId(@NotNull final Map<String, String> params) {
    return StringUtils.isNotBlank(params.get(AwsCloudConnectorConstants.CHOSEN_AWS_CONN_ID_PARAM));
  }
}
