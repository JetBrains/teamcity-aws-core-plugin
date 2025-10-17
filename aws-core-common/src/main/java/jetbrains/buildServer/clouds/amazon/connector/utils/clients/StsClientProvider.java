package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sts.StsClient;

public interface StsClientProvider {
  @NotNull
  StsClient getClientWithCredentials(@NotNull final AwsConnectionCredentials awsConnectionCredentials, @Nullable final Map<String, String> parameters)
    throws ConnectionCredentialsException;

  @NotNull
  StsClient getClient(@Nullable final Map<String, String> parameters);
}
