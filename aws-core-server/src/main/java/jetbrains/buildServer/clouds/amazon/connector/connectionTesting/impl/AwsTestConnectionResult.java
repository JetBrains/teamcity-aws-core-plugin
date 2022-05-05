package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

public class AwsTestConnectionResult {
  private final GetCallerIdentityResult myGetCallerIdentityResult;

  public AwsTestConnectionResult(GetCallerIdentityResult getCallerIdentityResult) {
    myGetCallerIdentityResult = getCallerIdentityResult;
  }

  public GetCallerIdentityResult getGetCallerIdentityResult() {
    return myGetCallerIdentityResult;
  }
}
