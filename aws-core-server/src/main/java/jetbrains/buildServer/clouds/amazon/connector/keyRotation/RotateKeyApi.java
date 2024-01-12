

package jetbrains.buildServer.clouds.amazon.connector.keyRotation;

import jetbrains.buildServer.clouds.amazon.connector.errors.KeyRotationException;

public interface RotateKeyApi {
  void rotateKey() throws KeyRotationException;
}