

package jetbrains.buildServer.clouds.amazon.connector.utils;

import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionUtils {

  @NotNull
  public static AwsCredentialsData getDataFromCredentials(@NotNull final Credentials credentials) {
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getSecretAccessKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return credentials.getSessionToken();
      }
    };
  }
}