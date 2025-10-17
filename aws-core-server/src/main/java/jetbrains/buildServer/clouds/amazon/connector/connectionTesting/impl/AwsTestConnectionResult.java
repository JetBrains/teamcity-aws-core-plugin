package jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl;

import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

public class AwsTestConnectionResult {
  private final GetCallerIdentityResponse myGetCallerIdentityResult;

  public AwsTestConnectionResult(GetCallerIdentityResponse getCallerIdentityResult) {
    myGetCallerIdentityResult = getCallerIdentityResult;
  }

  public GetCallerIdentityResponse getGetCallerIdentityResult() {
    return myGetCallerIdentityResult;
  }
}
