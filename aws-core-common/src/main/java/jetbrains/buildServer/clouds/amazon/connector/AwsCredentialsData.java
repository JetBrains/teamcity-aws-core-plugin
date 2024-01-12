

package jetbrains.buildServer.clouds.amazon.connector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AwsCredentialsData {
  @NotNull
  String getAccessKeyId();

  @NotNull
  String getSecretAccessKey();

  @Nullable
  String getSessionToken();
}