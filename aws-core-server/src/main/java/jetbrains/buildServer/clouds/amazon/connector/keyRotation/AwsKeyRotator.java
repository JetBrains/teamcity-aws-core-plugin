

package jetbrains.buildServer.clouds.amazon.connector.keyRotation;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

public interface AwsKeyRotator {
  void rotateConnectionKeys(@NotNull final String connectionId, @NotNull final SProject project) throws KeyRotationException;
}