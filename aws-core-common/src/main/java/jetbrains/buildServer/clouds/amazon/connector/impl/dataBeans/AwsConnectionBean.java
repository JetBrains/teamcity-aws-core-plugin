package jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionBean {
  private final String myConnectionId;
  private final String myDescription;
  private final AWSCredentialsProvider myCredentialsProvider;
  private final String myRegion;
  private final boolean myUsingSessionCredentials;

  public AwsConnectionBean(@NotNull final String connectionId,
                           @NotNull final String description,
                           @NotNull final AWSCredentialsProvider credentialsProvider,
                           @NotNull final String region,
                           final boolean usingSessionCredentials) {
    myConnectionId = connectionId;
    myDescription = description;
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

  @NotNull
  public String getDescription() {
    return myDescription;
  }

  public boolean isUsingSessionCredentials() {
    return myUsingSessionCredentials;
  }
}