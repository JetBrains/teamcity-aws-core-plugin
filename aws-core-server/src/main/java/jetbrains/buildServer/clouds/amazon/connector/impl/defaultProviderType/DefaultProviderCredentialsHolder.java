

package jetbrains.buildServer.clouds.amazon.connector.impl.defaultProviderType;

import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class DefaultProviderCredentialsHolder implements AwsCredentialsHolder {

  private final SProjectFeatureDescriptor connectionFeatureDescriptor;

  public DefaultProviderCredentialsHolder(@NotNull final SProjectFeatureDescriptor featureDescriptor) {
    connectionFeatureDescriptor = featureDescriptor;
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() throws AwsConnectorException {
    AwsCredentials credentials = constructNewDefaultProviderCredentials();
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
        if (credentials instanceof AwsSessionCredentials) {
          return ((AwsSessionCredentials) credentials).sessionToken();
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

  private AwsCredentials constructNewDefaultProviderCredentials() throws AwsConnectorException {
    try (DefaultCredentialsProvider dcp = DefaultCredentialsProvider.builder().build()) {
      return dcp.resolveCredentials();
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