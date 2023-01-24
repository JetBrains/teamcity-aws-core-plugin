package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsDescriptor;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionDescriptor extends ConnectionCredentialsDescriptor<AwsCredentialsData> {

  @NotNull
  @Deprecated
  AwsCredentialsHolder getAwsCredentialsHolder();

  @NotNull
  String getDescription();

  @NotNull
  String getRegion();

  boolean isUsingSessionCredentials();
}
