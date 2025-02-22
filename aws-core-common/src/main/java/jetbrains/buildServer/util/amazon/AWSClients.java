

package jetbrains.buildServer.util.amazon;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codepipeline.AWSCodePipelineClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions.AWSRegions;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
@Deprecated
public class AWSClients {

  @Nullable private final AWSCredentials myCredentials;
  @Nullable private String myServiceEndpoint;
  @Nullable private String myS3SignerType;
  @NotNull private final String myRegion;
  @NotNull private final ClientConfiguration myClientConfiguration;
  private boolean myDisablePathStyleAccess = false;

  private boolean myAccelerateModeEnabled = false;

  private AWSClients(@Nullable AWSCredentials credentials, @NotNull String region) {
    myCredentials = credentials;
    myRegion = region;
    myClientConfiguration = AWSCommonParams.createClientConfiguration();
  }

  @Nullable
  public AWSCredentials getCredentials() {
    return myCredentials;
  }

  @NotNull
  public ClientConfiguration getClientConfiguration() {
    return myClientConfiguration;
  }

  @NotNull
  public static AWSClients fromDefaultCredentialProviderChain(@NotNull String region) {
    return fromExistingCredentials(null, region);
  }
  @NotNull
  public static AWSClients fromBasicCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String region) {
    return fromExistingCredentials(new BasicAWSCredentials(accessKeyId, secretAccessKey), region);
  }

  @NotNull
  public static AWSClients fromBasicSessionCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String sessionToken, @NotNull String region) {
    return fromExistingCredentials(new BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken), region);
  }

  @NotNull
  public static AWSClients fromSessionCredentials(@NotNull final String accessKeyId, @NotNull final String secretAccessKey,
                                                  @NotNull final String iamRoleARN, @Nullable final String externalID,
                                                  @NotNull final String sessionName, final int sessionDuration,
                                                  @NotNull final String region) {
    return fromExistingCredentials(new AWSCommonParams.LazyCredentials() {
      @NotNull
      @Override
      protected AWSSessionCredentials createCredentials() {
        return AWSClients.fromBasicCredentials(accessKeyId, secretAccessKey, region).createSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration);
      }
    }, region);
  }

  @NotNull
  public static AWSClients fromSessionCredentials(@NotNull final String iamRoleARN, @Nullable final String externalID,
                                                  @NotNull final String sessionName, final int sessionDuration,
                                                  @NotNull final String region) {
    return fromExistingCredentials(new AWSCommonParams.LazyCredentials() {
      @NotNull
      @Override
      protected AWSSessionCredentials createCredentials() {
        return AWSClients.fromDefaultCredentialProviderChain(region).createSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration);
      }
    }, region);
  }

  @NotNull
  private static AWSClients fromExistingCredentials(@Nullable AWSCredentials credentials, @NotNull String region) {
    return new AWSClients(credentials, region);
  }

  @NotNull
  public AmazonS3 createS3Client() {
    if (StringUtil.isNotEmpty(myS3SignerType)) {
      myClientConfiguration.withSignerOverride(myS3SignerType);
    }

    final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                                                               .withClientConfiguration(myClientConfiguration)
                                                               .withAccelerateModeEnabled(myAccelerateModeEnabled)
                                                               .withPathStyleAccessEnabled(!myDisablePathStyleAccess);

    if (myCredentials != null) {
      builder.withCredentials(new AWSStaticCredentialsProvider(myCredentials));
    }

    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(myServiceEndpoint, region));
    } else {
      builder.withRegion(region);
    }

    return builder.build();
  }

  @NotNull
  public AmazonCodeDeployClient createCodeDeployClient() {
    return withRegion(myCredentials == null ? new AmazonCodeDeployClient(myClientConfiguration) : new AmazonCodeDeployClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  public AWSCodePipelineClient createCodePipeLineClient() {
    return withRegion(myCredentials == null ? new AWSCodePipelineClient(myClientConfiguration) : new AWSCodePipelineClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  public AWSCodeBuildClient createCodeBuildClient() {
    return withRegion(myCredentials == null ? new AWSCodeBuildClient(myClientConfiguration) : new AWSCodeBuildClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  public AmazonCloudFront createCloudFrontClient(){
    final AmazonCloudFrontClientBuilder builder = AmazonCloudFrontClientBuilder.standard()
                                                                               .withClientConfiguration(myClientConfiguration);

    if (myCredentials != null) {
      builder.withCredentials(new AWSStaticCredentialsProvider(myCredentials));
    }

    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(myServiceEndpoint, region));
    } else {
      builder.withRegion(region);
    }

    return builder.build();
  }

  @NotNull
  private AWSSecurityTokenService createSecurityTokenService() {
    AWSSecurityTokenServiceClientBuilder builder = AWSSecurityTokenServiceClientBuilder
      .standard()
      .withRegion(getRegion())
      .withClientConfiguration(myClientConfiguration);
    if (myCredentials != null){
      builder.withCredentials(new StaticCredentialsProvider(myCredentials));
    }
    return builder.build();
  }

  @NotNull
  public String getRegion() {
    return myRegion;
  }

  public void setServiceEndpoint(@NotNull final String serviceEndpoint) {
    myServiceEndpoint = StringUtil.trimEnd(serviceEndpoint,"/");
  }

  public void setS3SignerType(@NotNull final String s3SignerType) {
    myS3SignerType = s3SignerType;
  }

  public void setDisablePathStyleAccess(final boolean disablePathStyleAccess) {
    myDisablePathStyleAccess = disablePathStyleAccess;
  }

  public void setAccelerateModeEnabled(final boolean accelerateModeEnabled) {
    myAccelerateModeEnabled = accelerateModeEnabled;
  }

  @NotNull
  private <T extends AmazonWebServiceClient> T withRegion(@NotNull T client) {
    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      Loggers.SERVER.debug("Region is not specified, using default region: " + AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    return client.withRegion(AWSRegions.getRegion(region));
  }

  @NotNull
  private AWSSessionCredentials createSessionCredentials(@NotNull String iamRoleARN, @Nullable String externalID, @NotNull String sessionName, int sessionDuration)
    throws AWSException {
    final AssumeRoleRequest assumeRoleRequest =
      new AssumeRoleRequest()
        .withRoleArn(iamRoleARN)
        .withRoleSessionName(AWSCommonParams.patchSessionName(sessionName))
        .withDurationSeconds(AWSCommonParams.patchSessionDuration(sessionDuration));

    if (StringUtil.isNotEmpty(externalID))
      assumeRoleRequest.setExternalId(externalID);

    try {
      final Credentials credentials = createSecurityTokenService().assumeRole(assumeRoleRequest).getCredentials();
      return new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken());
    } catch (Exception e) {
      throw new AWSException(e);
    }
  }

  public static final String UNSUPPORTED_SESSION_NAME_CHARS = "[^\\w+=,.@-]";
  public static final int MAX_SESSION_NAME_LENGTH = 64;

}
