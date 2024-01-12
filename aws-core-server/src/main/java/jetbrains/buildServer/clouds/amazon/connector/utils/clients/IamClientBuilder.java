

package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import org.jetbrains.annotations.NotNull;

public interface IamClientBuilder {
  @NotNull
  AmazonIdentityManagement createIamClient(@NotNull final String connectionRegion, @NotNull final AWSCredentialsProvider credentials);
}