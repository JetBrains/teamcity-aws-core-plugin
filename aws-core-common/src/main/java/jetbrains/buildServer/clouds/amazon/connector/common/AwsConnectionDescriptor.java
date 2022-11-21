package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.connections.common.ConnectionCredentialsDescriptor;
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
