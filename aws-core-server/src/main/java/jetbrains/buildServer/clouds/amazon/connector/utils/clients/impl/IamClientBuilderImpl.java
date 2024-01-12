

package jetbrains.buildServer.clouds.amazon.connector.utils.clients.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.IamClientBuilder;
import org.jetbrains.annotations.NotNull;

public class IamClientBuilderImpl implements IamClientBuilder {
  @NotNull
  @Override
  public AmazonIdentityManagement createIamClient(@NotNull String connectionRegion, @NotNull AWSCredentialsProvider credentials) {
    return AmazonIdentityManagementClientBuilder
      .standard()
      .withRegion(Regions.fromName(connectionRegion))
      .withCredentials(credentials)
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"))
      .build();
  }
}