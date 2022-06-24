/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.RotateKeyApi;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.MultiNodeTasks;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

public class AwsKeyRotatorImpl implements AwsKeyRotator {

  private static final int ROTATE_TIMEOUT_SEC = 30;
  private static final TemporalAmount OLD_KEY_PRESERVE_TIME = Duration.ofDays(1);
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;
  private final OldKeysCleaner myOldKeysCleaner;

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final SecurityContextEx securityContext,
                           @NotNull final ConfigActionFactory configActionFactory,
                           @NotNull MultiNodeTasks multiNodeTasks) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;

    myOldKeysCleaner = createOldKeysCleaner(multiNodeTasks);
  }

  public void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws KeyRotationException {
    OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (awsConnectionDescriptor == null) {
      throw new KeyRotationException("The AWS Connection with ID " + connectionId + " was not found.");
    }

    String previousAccessKey = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
    Loggers.CLOUD.info("Key rotation initiated for the AWS key: " + ParamUtil.maskKey(previousAccessKey));
    RotateKeyApi rotateKeyApi = createRotateKeyApi(awsConnectionDescriptor, project);
    rotateKeyApi.rotateKey();

    myOldKeysCleaner.scheduleAwsKeyForDeletion(previousAccessKey, rotateKeyApi);
  }

  @NotNull
  protected RotateKeyApi createRotateKeyApi(@NotNull final OAuthConnectionDescriptor awsConnectionDescriptor, @NotNull final SProject project) {
    return new AwsRotateKeyApi(
      myOAuthConnectionsManager,
      mySecurityContext,
      myConfigActionFactory,
      awsConnectionDescriptor,
      project,
      ROTATE_TIMEOUT_SEC
    );
  }

  @NotNull
  protected OldKeysCleaner createOldKeysCleaner(@NotNull MultiNodeTasks multiNodeTasks) {
    return new OldKeysCleaner(
      multiNodeTasks,
      OLD_KEY_PRESERVE_TIME
    );
  }
}