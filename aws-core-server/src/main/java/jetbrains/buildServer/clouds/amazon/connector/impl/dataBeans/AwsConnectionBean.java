package jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionBean {
  private final String myConnectionId;
  private final AWSCredentialsProvider myCredentialsProvider;
  private final String myRegion;
  private final boolean myUsingSessionCredentials;

  public AwsConnectionBean(@NotNull final String connectionId,
                           @NotNull final AWSCredentialsProvider credentialsProvider,
                           @NotNull final String region,
                           final boolean usingSessionCredentials) {
    myConnectionId = connectionId;
    myCredentialsProvider = credentialsProvider;
    myRegion = region;
    myUsingSessionCredentials = usingSessionCredentials;
  }

  @NotNull
  public String getConnectionId() {
    return myConnectionId;
  }

  @NotNull
  public AWSCredentialsProvider getCredentialsProvider() {
    return myCredentialsProvider;
  }

  @NotNull
  public String getRegion() {
    return myRegion;
  }

  public boolean isUsingSessionCredentials() {
    return myUsingSessionCredentials;
  }
}