package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionDescriptor extends SProjectFeatureDescriptor {
  @NotNull
  AwsCredentialsHolder getAwsCredentialsHolder();

  @NotNull
  String getDescription();

  @NotNull
  String getRegion();

  boolean isUsingSessionCredentials();
}
