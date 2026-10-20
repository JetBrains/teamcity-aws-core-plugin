

package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.iam.IamClient;

public interface IamClientBuilder {
  @NotNull
  IamClient createIamClient(@NotNull final String connectionRegion, @NotNull final AwsCredentialsProvider credentials);
}