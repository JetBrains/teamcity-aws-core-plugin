

package jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.AwsKeyRotator;
import jetbrains.buildServer.clouds.amazon.connector.keyRotation.RotateKeyApi;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

public class AwsKeyRotatorImpl implements AwsKeyRotator {

  private static final int ROTATE_TIMEOUT_SEC = 30;
  private final OAuthConnectionsManager myOAuthConnectionsManager;
  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;
  private final OldKeysCleaner myOldKeysCleaner;

  public AwsKeyRotatorImpl(@NotNull final OAuthConnectionsManager oAuthConnectionsManager,
                           @NotNull final SecurityContextEx securityContext,
                           @NotNull final ConfigActionFactory configActionFactory,
                           @NotNull final OldKeysCleaner oldKeysCleaner) {
    myOAuthConnectionsManager = oAuthConnectionsManager;
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;

    myOldKeysCleaner = oldKeysCleaner;
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

    myOldKeysCleaner.scheduleAwsKeyForDeletion(previousAccessKey, connectionId, project.getExternalId());
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
}