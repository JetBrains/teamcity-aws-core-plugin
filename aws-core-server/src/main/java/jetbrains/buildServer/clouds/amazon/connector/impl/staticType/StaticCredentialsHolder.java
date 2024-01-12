

package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StaticCredentialsHolder implements AwsCredentialsHolder {

  private final String accessKeyId;
  private final String secretAccessKey;

  public StaticCredentialsHolder(String accessKeyId, String secretAccessKey) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
  }


  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return accessKeyId;
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return secretAccessKey;
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return null;
      }
    };
  }

  @Override
  public void refreshCredentials() {
    //...
  }

  @Nullable
  @Override
  public Date getSessionExpirationDate() {
    return null;
  }
}