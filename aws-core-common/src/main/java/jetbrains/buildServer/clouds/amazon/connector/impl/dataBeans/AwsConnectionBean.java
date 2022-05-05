package jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionBean {
  private final String myConnectionId;
  private final String myDescription;
  private final AwsCredentialsHolder myAwsCredentialsHolder;
  private final String myRegion;
  private final boolean myUsingSessionCredentials;

  public AwsConnectionBean(@NotNull final String connectionId,
                           @NotNull final String description,
                           @NotNull final AwsCredentialsHolder credentialsHolder,
                           @NotNull final String region,
                           final boolean usingSessionCredentials) {
    myConnectionId = connectionId;
    myDescription = description;
    myAwsCredentialsHolder = credentialsHolder;
    myRegion = region;
    myUsingSessionCredentials = usingSessionCredentials;
  }

  @NotNull
  public String getConnectionId() {
    return myConnectionId;
  }

  @NotNull
  public AwsCredentialsHolder getAwsCredentialsHolder() {
    return myAwsCredentialsHolder;
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