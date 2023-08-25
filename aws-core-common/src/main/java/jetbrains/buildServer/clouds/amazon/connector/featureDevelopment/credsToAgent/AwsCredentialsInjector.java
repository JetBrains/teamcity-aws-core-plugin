/*
 * Copyright 2000-2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

public class AwsCredentialsInjector {

  /**
   * Adds specific AWS Credentials to the Build Context via corresponding properties.
   * Use this method when there is one AWS Connection and its credentials should be available when running a build.
   *
   * @param context     A Build Context where to inject AWS credentials.
   * @param credentials AWS credentials to be injected.
   */
  public void injectCredentials(BuildStartContext context, AwsConnectionCredentials credentials) {
    String encodedCredentials = credentialsToEncodedString(Collections.singletonList(credentials));

    if (credentials.getAccessKeyId() != null) {
      context.addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, credentials.getAccessKeyId());
    }
    context.addSharedParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, encodedCredentials);
  }

  /**
   * Adds several AWS Credentials to the Build Context via corresponding properties using different AWS Profiles.
   * Use this method when there are multiple AWS Connections with different AWS Profile names. Basically used only when there are multiple AWS Credentials Build Features.
   *
   * @param context     A Build Context where to inject AWS credentials.
   * @param credentials AWS credentials to be injected.
   * @throws ConnectionCredentialsException thrown when there are duplicated AWS Profile names provided.
   */
  public void injectMultipleCredentials(BuildStartContext context, List<AwsConnectionCredentials> awsConnectionCredentials) throws ConnectionCredentialsException {
    throwExceptionIfDuplicatedProfileNames(awsConnectionCredentials);

    context.addSharedParameter(
      AwsConnBuildFeatureParams.INJECTED_AWS_ACCESS_KEYS,
      awsConnectionCredentials
        .stream()
        .map(AwsConnectionCredentials::getAccessKeyId)
        .collect(Collectors.joining(",", "", ""))
    );

    context.addSharedParameter(
      AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT,
      credentialsToEncodedString(awsConnectionCredentials)
    );
  }


  @NotNull
  private String credentialsToEncodedString(@NotNull final List<AwsConnectionCredentials> awsConnectionCredentials) {
    StringBuilder stringBuilder = new StringBuilder();
    for (AwsConnectionCredentials credentials : awsConnectionCredentials) {
      Map<String, String> parameters = getConnectionParametersToInject(credentials);

      String awsProfileName = credentials.getAwsProfileName() != null ? credentials.getAwsProfileName() : "default";
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

  private void throwExceptionIfDuplicatedProfileNames(@NotNull final List<AwsConnectionCredentials> awsConnectionCredentials) throws ConnectionCredentialsException {
    Map<String, List<AwsConnectionCredentials>> groupedByAwsProfiles = awsConnectionCredentials
      .stream()
      .collect(Collectors.groupingBy(credentials -> {
        String profileName = credentials.getAwsProfileName();
        return profileName != null ? profileName : "default";
      }));

    Map<String, List<AwsConnectionCredentials>> nonUniqueProfiles = groupedByAwsProfiles
      .entrySet().stream()
      .filter(entry -> entry.getValue().size() > 1)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!nonUniqueProfiles.isEmpty()) {
      throw new ConnectionCredentialsException(createErrorMsgForDuplicatedProfileNames(nonUniqueProfiles));
    }
  }

  private String createErrorMsgForDuplicatedProfileNames(Map<String, List<AwsConnectionCredentials>> nonUniqueProfiles) {
    return nonUniqueProfiles
      .keySet().stream()
      .map(profileName -> "<" + profileName + ">")
      .collect(Collectors.joining(",", "There are duplicated AWS Profile names: ", "\n"));
  }
}
