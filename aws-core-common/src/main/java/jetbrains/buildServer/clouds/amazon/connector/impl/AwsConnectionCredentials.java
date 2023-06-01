package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.*;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsConnectionCredentialsConstants;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionCredentials implements ConnectionCredentials {
  @Nullable
  private final String myAwsRegion;

  @Nullable
  private final String myAccessKeyId;
  @Nullable
  private final String mySecretAccessKey;
  @Nullable
  private final String mySessionToken;

  public AwsConnectionCredentials(@NotNull ConnectionCredentials connectionCredentials) {
    myAwsRegion = connectionCredentials.getProperties().get(AwsConnectionCredentialsConstants.REGION);

    myAccessKeyId = connectionCredentials.getProperties().get(AwsConnectionCredentialsConstants.ACCESS_KEY_ID);
    mySecretAccessKey = connectionCredentials.getProperties().get(AwsConnectionCredentialsConstants.SECRET_ACCESS_KEY);

    mySessionToken = connectionCredentials.getProperties().get(AwsConnectionCredentialsConstants.SESSION_TOKEN);
  }

  public AwsConnectionCredentials(@NotNull AwsCredentialsData awsCredentialsData, @NotNull Map<String, String> properties) {
    myAwsRegion = properties.get(AwsConnectionCredentialsConstants.REGION);

    myAccessKeyId = awsCredentialsData.getAccessKeyId();
    mySecretAccessKey = awsCredentialsData.getSecretAccessKey();

    mySessionToken = awsCredentialsData.getSessionToken();
  }

  @NotNull
  public AWSCredentialsProvider toAWSCredentialsProvider() throws ConnectionCredentialsException {
    if (myAccessKeyId == null || mySecretAccessKey == null) {
      throw new AwsConnectorException("Connection credentials were not provided");
    }
    if (myAwsRegion == null) {
      throw new AwsConnectorException("Connection region was not provided");
    }

    AWSCredentials credentials;
    if (mySessionToken == null) {
      credentials = new BasicAWSCredentials(
        myAccessKeyId,
        mySecretAccessKey
      );
    } else {
      credentials = new BasicSessionCredentials(
        myAccessKeyId,
        mySecretAccessKey,
        mySessionToken
      );
    }

    return new AWSStaticCredentialsProvider(credentials);
  }

  @NotNull
  @Override
  public Map<String, String> getProperties() {
    Map<String, String> credsProps = new HashMap<>();

    if (myAccessKeyId != null) {
      credsProps.put(AwsConnectionCredentialsConstants.ACCESS_KEY_ID, myAccessKeyId);
    }
    if (mySecretAccessKey != null) {
      credsProps.put(AwsConnectionCredentialsConstants.SECRET_ACCESS_KEY, mySecretAccessKey);
    }
    if (mySessionToken != null) {
      credsProps.put(AwsConnectionCredentialsConstants.SESSION_TOKEN, mySessionToken);
    }
    if (myAwsRegion != null) {
      credsProps.put(AwsConnectionCredentialsConstants.REGION, myAwsRegion);
    }

    return credsProps;
  }

  @NotNull
  @Override
  public String getProviderType() {
    return AwsCloudConnectorConstants.CLOUD_TYPE;
  }

  /**
   * Returns default AWS region in case of setting isn't provided.
   * @return AWS region name
   */
  @NotNull
  public String getAwsRegion() {
    if (myAwsRegion == null) {
      return AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    return myAwsRegion;
  }

  @Nullable
  public String getAccessKeyId() {
    return myAccessKeyId;
  }

  @Nullable
  public String getSecretAccessKey() {
    return mySecretAccessKey;
  }

  @Nullable
  public String getSessionToken() {
    return mySessionToken;
  }
}
