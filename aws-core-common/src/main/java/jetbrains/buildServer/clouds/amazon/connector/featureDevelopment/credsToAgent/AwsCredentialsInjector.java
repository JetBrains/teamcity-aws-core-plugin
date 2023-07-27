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

import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnBuildFeatureParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AwsCredentialsInjector {
  private final LinkedAwsConnectionProvider myLinkedAwsConnectionProvider;

  public AwsCredentialsInjector(LinkedAwsConnectionProvider linkedAwsConnectionProvider) {
    myLinkedAwsConnectionProvider = linkedAwsConnectionProvider;
  }

  public void injectCredentials(BuildStartContext context){
      try {
        List<ConnectionCredentials> linkedAwsConnectionCredentials = myLinkedAwsConnectionProvider.getConnectionCredentialsFromBuild(context.getBuild());
        if (linkedAwsConnectionCredentials.isEmpty()) {
          reportProblem(context, String.format("Build with id: <%s> has AWS Connection to inject, but none was added. Enable debug to see more information.", context.getBuild().getBuildId()));
          return;
        }

        //TODO: TW-75618 Add support for several AWS Connections exposing
        AwsConnectionCredentials credentials = new AwsConnectionCredentials(linkedAwsConnectionCredentials.stream().findFirst().get());
        Map<String, String> parameters = getConnectionParametersToExpose(credentials);

        String encodedCredentials = parametersToEncodedString(parameters);

        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, parameters.get(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM));
        context.addSharedParameter(AwsConnBuildFeatureParams.AWS_INTERNAL_ENCODED_CREDENTIALS_CONTENT, encodedCredentials);

      } catch (ConnectionCredentialsException e) {
        String warningMessage = "Failed to inject AWS Connection to a build: " + e.getMessage();
        Loggers.CLOUD.warnAndDebugDetails(warningMessage, e);
        reportProblem(context, warningMessage);
      }
  }


  @NotNull
  private String parametersToEncodedString(@NotNull Map<String, String> parameters) {
    String prefix = "[default]" + "\n";
    String credentials = parameters.entrySet().stream()
      .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
      .collect(Collectors.joining("\n", prefix, ""));

    return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  @NotNull
  private Map<String, String> getConnectionParametersToExpose(@NotNull final AwsConnectionCredentials credentials) {
    Map<String, String> parameters = new HashMap<>();

    parameters.put(AwsConnBuildFeatureParams.AWS_REGION_CONFIG_FILE_PARAM, credentials.getAwsRegion());

    parameters.put(AwsConnBuildFeatureParams.AWS_ACCESS_KEY_CONFIG_FILE_PARAM, credentials.getAccessKeyId());
    parameters.put(AwsConnBuildFeatureParams.AWS_SECRET_KEY_CONFIG_FILE_PARAM, credentials.getSecretAccessKey());
    if (credentials.getSessionToken() != null) {
      parameters.put(AwsConnBuildFeatureParams.AWS_SESSION_TOKEN_CONFIG_FILE_PARAM, credentials.getSessionToken());
    }
    return parameters;
  }

  private void reportProblem(@NotNull BuildStartContext context, @NotNull String message) {
    context
      .getBuild()
      .getBuildLog()
      .messageAsync(
        message,
        Status.WARNING,
        MessageAttrs.fromMessage(DefaultMessagesInfo.createTextMessage(message))
      );
  }
}
