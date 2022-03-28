package jetbrains.buildServer.clouds.amazon.connector;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.InvalidProperty;
import org.jetbrains.annotations.NotNull;

public interface AwsConnectorFactory {

  /**
   * Creates <b>AWSCredentialsProvider</b> using connection properties.
   * The credentials type will be extracted from the parameters.
   * @param  connectionProperties  Connection feature properties.
   * @return AWSCredentialsProvider object with needed credentials type.
   */
  @NotNull
  public AWSCredentialsProvider buildAwsCredentialsProvider(@NotNull final Map<String, String> connectionProperties);

  void registerAwsCredentialsBuilder(@NotNull final AwsCredentialsBuilder credentialsBuilder);

  @NotNull
  List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties);

  @NotNull
  String describeAwsConnection(@NotNull final Map<String, String> connectionProperties);
}
