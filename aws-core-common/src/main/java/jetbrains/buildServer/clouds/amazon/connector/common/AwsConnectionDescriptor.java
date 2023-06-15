package jetbrains.buildServer.clouds.amazon.connector.common;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.serverSide.connections.ConnectionDescriptor;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionDescriptor extends ConnectionDescriptor {
  @NotNull
  AwsCredentialsHolder getAwsCredentialsHolder();
  @NotNull
  String getRegion();
}
