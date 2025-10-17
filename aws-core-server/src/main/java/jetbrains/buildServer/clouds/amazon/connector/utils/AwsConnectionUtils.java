

package jetbrains.buildServer.clouds.amazon.connector.utils;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sts.model.Credentials;

public class AwsConnectionUtils {

  @NotNull
  public static AwsCredentialsData getDataFromCredentials(@NotNull final Credentials credentials) {
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.accessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.secretAccessKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return credentials.sessionToken();
      }
    };
  }
}