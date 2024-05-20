

package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildStartContext;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil.getAwsProfileNameOrDefault;

public class AwsCredentialsInjector {

  /**
   * Adds specific AWS Credentials to the Build Context via corresponding properties.
   * Use this method when there is one AWS Connection and its credentials should be available when running a build.
   *
   * @param context     A Build Context where to inject AWS credentials.
   * @param credentials AWS credentials to be injected.
   */
  public void injectCredentials(BuildStartContext context, AwsConnectionCredentials credentials) {
    //TW-75618 collection for multiple AWS Connection injection
    String encodedCredentials = credentialsToEncodedString(Collections.singletonList(credentials));

    if (credentials.getAccessKeyId() != null) {
      context.addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, credentials.getAccessKeyId());
    }

    String awsProfileName = getAwsProfileNameOrDefault(credentials.getAwsProfileName());
    context.addSharedParameter(AwsConnBuildFeatureParams.AWS_PROFILE_NAME_PARAM, awsProfileName);

    context.addSharedParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, encodedCredentials);
  }


  @NotNull
  private String credentialsToEncodedString(@NotNull final List<AwsConnectionCredentials> awsConnectionCredentials) {
    StringBuilder stringBuilder = new StringBuilder();
    for (AwsConnectionCredentials credentials : awsConnectionCredentials) {
      Map<String, String> parameters = getConnectionParametersToInject(credentials);

      String awsProfileName = getAwsProfileNameOrDefault(credentials.getAwsProfileName());
      String prefix = "[" + awsProfileName + "]" + "\n";
      String awsCredentialsFileEntry = parameters
        .entrySet()
        .stream()
        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("\n", prefix, "\n"));

      stringBuilder.append(awsCredentialsFileEntry);
    }

    return Base64.getEncoder().encodeToString(
      stringBuilder.toString().getBytes(StandardCharsets.UTF_8)
    );
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