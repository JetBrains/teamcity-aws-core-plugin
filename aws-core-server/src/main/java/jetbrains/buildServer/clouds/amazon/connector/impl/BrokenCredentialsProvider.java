package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class BrokenCredentialsProvider implements AWSCredentialsProvider {

  public BrokenCredentialsProvider(){
    Loggers.CLOUD.info("Broken AWS Credentials provider has been created (with empty access and secret key).");
  }

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
