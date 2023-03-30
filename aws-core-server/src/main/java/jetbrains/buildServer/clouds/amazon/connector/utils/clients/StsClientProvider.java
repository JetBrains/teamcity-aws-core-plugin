package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectionCredentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StsClientProvider {
  @NotNull
  AWSSecurityTokenService getClientWithCredentials(@NotNull final AwsConnectionCredentials awsConnectionCredentials, @Nullable final Map<String, String> parameters)
    throws AwsConnectorException;

  @NotNull
  AWSSecurityTokenService getClient(@Nullable final Map<String, String> parameters);
}
