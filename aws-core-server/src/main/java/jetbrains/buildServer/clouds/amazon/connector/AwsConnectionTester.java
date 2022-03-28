package jetbrains.buildServer.clouds.amazon.connector;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectionTester {
  @NotNull
  GetCallerIdentityResult testConnection(@NotNull final Map<String, String> connectionProperties) throws AmazonClientException;
  @NotNull
  List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties);
}
