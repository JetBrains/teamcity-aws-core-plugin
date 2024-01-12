

package jetbrains.buildServer.clouds.amazon.connector.impl.defaultProviderType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultProviderCredentialsHolder implements AwsCredentialsHolder {

  private final SProjectFeatureDescriptor connectionFeatureDescriptor;

  public DefaultProviderCredentialsHolder(@NotNull final SProjectFeatureDescriptor featureDescriptor) {
    connectionFeatureDescriptor = featureDescriptor;
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() throws AwsConnectorException {
    AWSCredentials credentials = constructNewDefaultProviderCredentials();
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAWSAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getAWSSecretKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        if (credentials instanceof AWSSessionCredentials) {
          return ((AWSSessionCredentials)credentials).getSessionToken();
        } else {
          return null;
        }
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

  private AWSCredentials constructNewDefaultProviderCredentials() throws AwsConnectorException {
    try {
      return new DefaultAWSCredentialsProviderChain().getCredentials();

    } catch (Exception e) {
      String errorMsg = String.format(
        "Failed to use the DefaultAWSCredentialsProviderChain, Connection ID: %s, project ID: %s, reason %s",
        connectionFeatureDescriptor.getId(),
        connectionFeatureDescriptor.getProjectId(),
        e.getMessage()
      );

      throw new AwsConnectorException(errorMsg);
    }
  }
}