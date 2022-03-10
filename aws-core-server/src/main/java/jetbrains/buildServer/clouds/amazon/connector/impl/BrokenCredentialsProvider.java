package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.jetbrains.annotations.NotNull;

public class BrokenCredentialsProvider implements AWSCredentialsProvider {

  @Override
  @NotNull
  public AWSCredentials getCredentials() {
    return new BasicAWSCredentials(
      "",
      ""
    );
  }

  @Override
  public void refresh() {
    //...
  }
}
