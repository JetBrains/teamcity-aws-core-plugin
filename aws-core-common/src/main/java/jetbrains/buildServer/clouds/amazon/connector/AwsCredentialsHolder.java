

package jetbrains.buildServer.clouds.amazon.connector;

import java.util.Date;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AwsCredentialsHolder {

  @NotNull
  AwsCredentialsData getAwsCredentials() throws ConnectionCredentialsException;

  void refreshCredentials();

  @Nullable
  Date getSessionExpirationDate();

}