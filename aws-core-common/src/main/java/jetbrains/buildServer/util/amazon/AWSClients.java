

package jetbrains.buildServer.util.amazon;

import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions.AWSRegions;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.CloudFrontClientBuilder;
import software.amazon.awssdk.services.codebuild.CodeBuildClient;
import software.amazon.awssdk.services.codedeploy.CodeDeployClient;
import software.amazon.awssdk.services.codepipeline.CodePipelineClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vbedrosova
 */
@Deprecated
public class AWSClients {

  @Nullable private final AwsCredentials myCredentials;
  @Nullable private String myServiceEndpoint;
  @Nullable private String myS3SignerType;
  @Nullable private ConnectionSocketFactory mySocketFactory;
  @NotNull private final String myRegion;
  private boolean myDisablePathStyleAccess = false;

  private boolean myAccelerateModeEnabled = false;
  private static final Map<String, Signer> SIGNERS = new HashMap<>();
  private static final Signer DEFAULT;
  static {
    DEFAULT = AwsS3V4Signer.create();
    SIGNERS.put("Aws4Signer", Aws4Signer.create());
    SIGNERS.put("AwsS3V4Signer", DEFAULT);
  }

  private AWSClients(@Nullable AwsCredentials credentials, @NotNull String region) {
    myCredentials = credentials;
    myRegion = region;
  }

  @Nullable
  public AwsCredentials getCredentials() {
    return myCredentials;
  }

  @NotNull
  public static AWSClients fromDefaultCredentialProviderChain(@NotNull String region) {
    return fromExistingCredentials(null, region);
  }
  @NotNull
  public static AWSClients fromBasicCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String region) {
    return fromExistingCredentials(AwsBasicCredentials.create(accessKeyId, secretAccessKey), region);
  }

  @NotNull
  public static AWSClients fromBasicSessionCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String sessionToken, @NotNull String region) {
    return fromExistingCredentials(AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken), region);
  }

  @NotNull
  public static AWSClients fromSessionCredentials(@NotNull final String accessKeyId, @NotNull final String secretAccessKey,
                                                  @NotNull final String iamRoleARN, @Nullable final String externalID,
                                                  @NotNull final String sessionName, final int sessionDuration,
                                                  @NotNull final String region) {
    return fromExistingCredentials(new AWSCommonParams.LazyCredentials() {
      @NotNull
      @Override
      protected AwsSessionCredentials createCredentials() {
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
      protected AwsSessionCredentials createCredentials() {
        return AWSClients.fromDefaultCredentialProviderChain(region).createSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration);
      }
    }, region);
  }

  @NotNull
  private static AWSClients fromExistingCredentials(@Nullable AwsCredentials credentials, @NotNull String region) {
    return new AWSClients(credentials, region);
  }

  @NotNull
  public S3Client createS3Client() {
    ClientOverrideConfiguration.Builder overrideConfigurationBuilder = ClientConfigurationBuilder.clientOverrideConfigurationBuilder();
    if (StringUtil.isNotEmpty(myS3SignerType)) {
      overrideConfigurationBuilder.putAdvancedOption(SdkAdvancedClientOption.SIGNER, SIGNERS.getOrDefault(myS3SignerType, DEFAULT));
    }

    S3ClientBuilder builder = S3Client.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder(null, mySocketFactory))
      .overrideConfiguration(overrideConfigurationBuilder.build())
      .serviceConfiguration(
        config -> config.accelerateModeEnabled(myAccelerateModeEnabled)
          .pathStyleAccessEnabled(!myDisablePathStyleAccess)
      );

    if (myCredentials != null) {
      builder.credentialsProvider(StaticCredentialsProvider.create(myCredentials));
    }

    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.endpointOverride(URI.create(myServiceEndpoint));
    }

    builder.region(Region.of(region));

    return builder.build();
  }

  public S3AsyncClient createS3AsyncClient(S3Util.S3AdvancedConfiguration advancedConfiguration, TlsTrustManagersProvider tlsTrustManagersProvider) {
    ClientOverrideConfiguration.Builder overrideConfigurationBuilder = ClientConfigurationBuilder.clientOverrideConfigurationBuilder();
    if (StringUtil.isNotEmpty(myS3SignerType)) {
      overrideConfigurationBuilder.putAdvancedOption(SdkAdvancedClientOption.SIGNER, SIGNERS.getOrDefault(myS3SignerType, DEFAULT));
    }

    S3AsyncClientBuilder builder = S3AsyncClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .httpClientBuilder(ClientConfigurationBuilder.createAsyncClientBuilder(null, tlsTrustManagersProvider))
      .overrideConfiguration(overrideConfigurationBuilder.build())
      .serviceConfiguration(
        config -> config.accelerateModeEnabled(myAccelerateModeEnabled)
          .pathStyleAccessEnabled(!myDisablePathStyleAccess)
      );

    if (advancedConfiguration != null) {
      builder.multipartConfiguration(
        multipart -> multipart.thresholdInBytes(advancedConfiguration.getMultipartUploadThreshold())
          .minimumPartSizeInBytes(advancedConfiguration.getMinimumUploadPartSize())
      );
    }

    if (myCredentials != null) {
      builder.credentialsProvider(StaticCredentialsProvider.create(myCredentials));
    }

    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.endpointOverride(URI.create(myServiceEndpoint));
    }

    builder.region(Region.of(region));

    return builder.build();
  }

  @NotNull
  public CodeDeployClient createCodeDeployClient() {
    return createSyncAwsClientWithCredentials(CodeDeployClient.builder());
  }

  @NotNull
  public CodePipelineClient createCodePipeLineClient() {
    return createSyncAwsClientWithCredentials(CodePipelineClient.builder());
  }

  @NotNull
  public CodeBuildClient createCodeBuildClient() {
    return createSyncAwsClientWithCredentials(CodeBuildClient.builder());
  }

  @NotNull
  public CloudFrontClient createCloudFrontClient(){
    final CloudFrontClientBuilder builder = CloudFrontClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .httpClientBuilder(ClientConfigurationBuilder.createClientBuilder(null, mySocketFactory))
      .overrideConfiguration(
        ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
          .build()
      );

    if (myCredentials != null) {
      builder.credentialsProvider(StaticCredentialsProvider.create(myCredentials));
    }

    // null in myRegion will cause S3 client instantiation to fail
    // we ensure, that we have at least default region
    String region = myRegion;

    if (myRegion == null) {
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.endpointOverride(URI.create(myServiceEndpoint));
    }

    builder.region(Region.of(region));

    return builder.build();
  }

  private <T extends AwsClientBuilder<T, C> & SdkSyncClientBuilder<T, C>, C extends AwsClient> C createSyncAwsClientWithCredentials(T awsClientBuilder) {
    awsClientBuilder.httpClientBuilder(ClientConfigurationBuilder.createClientBuilder())
      .overrideConfiguration(
        ClientConfigurationBuilder.clientOverrideConfigurationBuilder()
          .build()
      );

    if (myCredentials != null) {
      awsClientBuilder.credentialsProvider(StaticCredentialsProvider.create(myCredentials));
    }

    return awsClientBuilder.build();
  }

  @NotNull
  private StsClient createSecurityTokenService() {
    String region = myRegion;

    if (myRegion == null) {
      Loggers.SERVER.debug("Region is not specified, using default region: " + AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
      region = AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    StsClientBuilder builder = StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .region(AWSRegions.getRegion(region))
      .httpClientBuilder(AWSCommonParams.createClientBuilder())
      .overrideConfiguration(
        AWSCommonParams.clientOverrideConfigurationBuilder()
          .build()
      );

    if (myCredentials != null){
      builder.credentialsProvider(StaticCredentialsProvider.create(myCredentials));
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

  public void setSocketFactory(@NotNull final ConnectionSocketFactory socketFactory) {
    mySocketFactory = socketFactory;
  }

  public void setDisablePathStyleAccess(final boolean disablePathStyleAccess) {
    myDisablePathStyleAccess = disablePathStyleAccess;
  }

  public void setAccelerateModeEnabled(final boolean accelerateModeEnabled) {
    myAccelerateModeEnabled = accelerateModeEnabled;
  }

  @NotNull
  private AwsSessionCredentials createSessionCredentials(@NotNull String iamRoleARN, @Nullable String externalID, @NotNull String sessionName, int sessionDuration)
    throws AWSException {
    final AssumeRoleRequest.Builder assumeRoleRequestBuilder =
      AssumeRoleRequest.builder()
        .roleArn(iamRoleARN)
        .roleSessionName(AWSCommonParams.patchSessionName(sessionName))
        .durationSeconds(AWSCommonParams.patchSessionDuration(sessionDuration));

    if (StringUtil.isNotEmpty(externalID))
      assumeRoleRequestBuilder.externalId(externalID);

    AssumeRoleRequest assumeRoleRequest = assumeRoleRequestBuilder.build();
    try (StsClient stsClient = createSecurityTokenService()) {
      final Credentials credentials = stsClient.assumeRole(assumeRoleRequest).credentials();
      return AwsSessionCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());
    } catch (Exception e) {
      throw new AWSException(e);
    }
  }

  public static final String UNSUPPORTED_SESSION_NAME_CHARS = "[^\\w+=,.@-]";
  public static final int MAX_SESSION_NAME_LENGTH = 64;

}
