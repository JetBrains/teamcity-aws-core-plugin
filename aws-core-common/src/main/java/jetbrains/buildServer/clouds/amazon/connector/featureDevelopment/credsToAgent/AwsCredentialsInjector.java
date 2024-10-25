package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildStartContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.getAwsProfileNameOrDefault;

public class AwsCredentialsInjector {

  /**
   * Adds specific AWS Credentials to the Build Context via corresponding properties.
   * Use this method when there is one AWS Connection and its credentials should be available when running a build.
   * Can be used repeatedly for multiple connection credentials injection.
   *
   * @param context     A Build Context where to inject AWS credentials.
   * @param credentials AWS credentials to be injected.
   */
  public void injectCredentials(@NotNull final BuildStartContext context, @NotNull final AwsConnectionCredentials credentials, @Nullable final String awsProfileName) {
    appendContextParam(context, AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM, getAwsProfileNameOrDefault(awsProfileName));

    if (credentials.getAccessKeyId() != null) {
      appendContextParam(context, AwsConnBuildFeatureParams.INJECTED_AWS_ACCESS_KEYS, credentials.getAccessKeyId());
    }

    appendEncodedCredsContent(context, credentialsToConfigString(credentials, awsProfileName));
  }

  private void appendContextParam(@NotNull final BuildStartContext context, @NotNull final String paramKey, @NotNull final String paramValue) {
    String currentValue = context.getSharedParameters().get(paramKey);
    String newValue = paramValue;
    if (currentValue != null && !currentValue.isEmpty()) {
      newValue = currentValue + ", " + newValue;
    }

    context.addSharedParameter(paramKey, newValue);
  }

  private void appendEncodedCredsContent(@NotNull final BuildStartContext context, @NotNull final String credsContent) {
    String currentEncodedValue = context.getSharedParameters().get(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT);
    String newValue = credsContent;
    if (currentEncodedValue != null && !currentEncodedValue.isEmpty()) {
      newValue = new String(Base64.getDecoder().decode(currentEncodedValue), StandardCharsets.UTF_8) +
        newValue;
    }

    context.addSharedParameter(
      AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT,
      Base64.getEncoder().encodeToString(newValue.getBytes(StandardCharsets.UTF_8))
    );
  }


  @NotNull
  private String credentialsToConfigString(@NotNull final AwsConnectionCredentials awsConnectionCredentials, @Nullable final String awsProfileName) {
    Map<String, String> parameters = getConnectionParametersToInject(awsConnectionCredentials);

    String prefix = "[" + getAwsProfileNameOrDefault(awsProfileName) + "]" + "\n";
    return parameters
      .entrySet()
      .stream()
      .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
      .collect(Collectors.joining("\n", prefix, "\n"));
  }

  @NotNull
  private Map<String, String> getConnectionParametersToInject(@NotNull final AwsConnectionCredentials credentials) {
    Map<String, String> parameters = new HashMap<>();

    parameters.put(AwsConnBuildFeatureParams.AWS_REGION_CONFIG_FILE_PARAM, credentials.getAwsRegion());

    parameters.put(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, credentials.getAccessKeyId());
    parameters.put(AwsConnBuildFeatureParams.AWS_SECRET_KEY_CONFIG_FILE_PARAM, credentials.getSecretAccessKey());
    if (credentials.getSessionToken() != null) {
      parameters.put(AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_CONFIG_FILE_PARAM, credentials.getSessionToken());
    }
    return parameters;
  }

}