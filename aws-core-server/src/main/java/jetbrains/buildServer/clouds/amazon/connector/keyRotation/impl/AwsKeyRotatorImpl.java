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
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public class AwsKeyRotatorImpl extends BuildServerAdapter implements AwsKeyRotator {

  private static final int ROTATE_TIMEOUT_SEC = 30;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;

  private final ConcurrentHashMap<String, AwsRotateKeyActions> scheduledForDeletionKeys = new ConcurrentHashMap<>();

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final EventDispatcher<BuildServerListener> buildServerEventDispatcher,
                           @NotNull final SecurityContextEx securityContext,
                           @NotNull final ConfigActionFactory configActionFactory) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    buildServerEventDispatcher.addListener(this);
  }

  public void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws KeyRotationException {
    OAuthConnectionDescriptor awsConnectionDescriptor = myOAuthConnectionsManager.findConnectionById(project, connectionId);
    if (awsConnectionDescriptor == null) {
      throw new KeyRotationException("The AWS Connection with ID " + connectionId + " was not found.");
    }

    String previousAccessKey = awsConnectionDescriptor.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);
    Loggers.CLOUD.info("Key rotation initiated for the AWS key: " + ParamUtil.maskKey(previousAccessKey));
    AwsRotateKeyActions rotateActions = createRotateKeyActions(awsConnectionDescriptor, project);

    Loggers.CLOUD.debug("Creating a new key...");
    rotateActions.createNewKey();

    Loggers.CLOUD.debug("Waiting for the new key to become available...");
    rotateActions.waitUntilRotatedKeyIsAvailable();

    Loggers.CLOUD.debug("Updating the AWS Connection...");
    scheduledForDeletionKeys.put(previousAccessKey, rotateActions);
    rotateActions.updateConnection();
  }

  @NotNull
  protected AwsRotateKeyActions createRotateKeyActions(@NotNull final OAuthConnectionDescriptor awsConnectionDescriptor, @NotNull final SProject project){
    return new AwsRotateKeyActions(
      myOAuthConnectionsManager,
      mySecurityContext,
      myConfigActionFactory,
      awsConnectionDescriptor,
      project,
      ROTATE_TIMEOUT_SEC
    );
  }

  @Override
  public void projectFeatureChanged(@NotNull final SProject project, @NotNull final SProjectFeatureDescriptor before, @NotNull final SProjectFeatureDescriptor after) {
    if (!OAuthConstants.FEATURE_TYPE.equals(after.getType())) {
      return;
    }

    String previousAccessKeyId = before.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);

    if (previousAccessKeyId == null || !scheduledForDeletionKeys.containsKey(previousAccessKeyId)) {
      return;
    }
    String newAccessKeyId = after.getParameters().get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM);

    if (!newAccessKeyId.equals(previousAccessKeyId)) {
      try {
        Loggers.CLOUD.info("Deleting the previous key " + ParamUtil.maskKey(previousAccessKeyId));
        scheduledForDeletionKeys.get(previousAccessKeyId).deletePreviousAccessKey();

      } catch (KeyRotationException keyRotationException) {
        Loggers.CLOUD.warn("Failed to delete old AWS key after rotation: " + keyRotationException.getMessage());
      }

      scheduledForDeletionKeys.remove(previousAccessKeyId);
    }
  }
}