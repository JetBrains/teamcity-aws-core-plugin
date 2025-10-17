

package jetbrains.buildServer.util.amazon;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import static jetbrains.buildServer.serverSide.TeamCityProperties.getInteger;
import static jetbrains.buildServer.serverSide.TeamCityProperties.getPropertyOrNull;
import static jetbrains.buildServer.util.amazon.AWSClients.*;

/**
 * @author vbedrosova
 */
@Deprecated
public final class AWSCommonParams {

  // "codedeploy_" prefix is for backward compatibility

  private static final String DEFAULT_SUFFIX = "aws";
  private static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;

  public static final String ENVIRONMENT_NAME_PARAM = "aws.environment";
  public static final String ENVIRONMENT_NAME_LABEL = "AWS environment";
  public static final String ENVIRONMENT_TYPE_CUSTOM = "custom";

  public static final String SERVICE_ENDPOINT_PARAM = "aws.service.endpoint";
  public static final String SERVICE_ENDPOINT_LABEL = "Endpoint URL";

  public static final String REGION_NAME_PARAM_OLD = "codedeploy_region_name";
  public static final String REGION_NAME_PARAM = "aws.region.name";
  public static final String REGION_NAME_LABEL = "AWS region";

  public static final String CREDENTIALS_TYPE_PARAM_OLD = "codedeploy_credentials_type";
  public static final String CREDENTIALS_TYPE_PARAM = "aws.credentials.type";
  public static final String CREDENTIALS_TYPE_LABEL = "Credentials type";
  public static final String TEMP_CREDENTIALS_OPTION_OLD = "codedeploy_temp_credentials";
  public static final String TEMP_CREDENTIALS_OPTION = "aws.temp.credentials";
  public static final String TEMP_CREDENTIALS_LABEL = "Temporary credentials";
  public static final String ACCESS_KEYS_OPTION_OLD = "codedeploy_access_keys";
  public static final String ACCESS_KEYS_OPTION = "aws.access.keys";
  public static final String ACCESS_KEYS_LABEL = "Access keys";

  public static final String SSL_CERT_DIRECTORY_PARAM = "aws.ssl.cert.directory";

  public static final String DEFAULT_CREDENTIALS_PROVIDER_CHAIN_DISABLED_PARAM = "teamcity.internal.aws.disable.default.credential.provider.chain";
  // this option should hide the default credentials provider chain checkbox,
  // but allow its usage through for the existing connections and ones created through the DSL
  // mainly this intended to use for compatibility or in complex circumstances (i.e. TC Cloud)
  public static final String DEFAULT_CREDENTIALS_PROVIDER_CHAIN_HIDDEN_PARAM = "teamcity.internal.aws.hide.default.credential.provider.chain";
  public static final String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM_OLD = "use_default_credential_provider_chain";
  public static final String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM = "aws.use.default.credential.provider.chain";
  public static final String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_LABEL = "Default Credential Provider Chain";

  public static final String ACCESS_KEY_ID_PARAM_OLD = "codedeploy_access_key_id";
  public static final String ACCESS_KEY_ID_PARAM = "aws.access.key.id";
  public static final String ACCESS_KEY_ID_LABEL = "Access key ID";
  public static final String SECURE_SECRET_ACCESS_KEY_PARAM_OLD = "secure:codedeploy_secret_access_key";
  public static final String SECURE_SECRET_ACCESS_KEY_PARAM = "secure:aws.secret.access.key";
  public static final String SECRET_ACCESS_KEY_PARAM_OLD = "codedeploy_secret_access_key";
  public static final String SECRET_ACCESS_KEY_PARAM = "aws.secret.access.key";
  public static final String SECRET_ACCESS_KEY_LABEL = "Secret access key";

  public static final String IAM_ROLE_ARN_PARAM_OLD = "codedeploy_iam_role_arn";
  public static final String IAM_ROLE_ARN_PARAM = "aws.iam.role.arn";
  public static final String IAM_ROLE_ARN_LABEL = "IAM role ARN";
  public static final String EXTERNAL_ID_PARAM_OLD = "codedeploy_external_id";
  public static final String EXTERNAL_ID_PARAM = "aws.external.id";
  public static final String EXTERNAL_ID_LABEL = "External ID";

  private static final Map<String, String> DEFAULTS = Collections.unmodifiableMap(CollectionsUtil.asMap(
    CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
    EXTERNAL_ID_PARAM, UUID.randomUUID().toString(),
    USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM, "false",
    ENVIRONMENT_NAME_PARAM, "",
    SERVICE_ENDPOINT_PARAM, ""
  ));

  public static final String TEMP_CREDENTIALS_SESSION_NAME_PARAM = "aws.temp.credentials.session.name";
  public static final String TEMP_CREDENTIALS_SESSION_NAME_DEFAULT_PREFIX = "TeamCity_AWS_support_";
  public static final String TEMP_CREDENTIALS_DURATION_SEC_PARAM = "aws.temp.credentials.duration.sec";
  public static final int TEMP_CREDENTIALS_DURATION_SEC_DEFAULT = 1800;

  @NotNull
  public static Map<String, String> validate(@NotNull Map<String, String> params, boolean acceptReferences) {
    final Map<String, String> invalids = new HashMap<String, String>();

    if (StringUtil.isEmptyOrSpaces(getRegionName(params))) {
      invalids.put(REGION_NAME_PARAM, REGION_NAME_LABEL + " must not be empty");
    }

    if (!isUseDefaultCredentialProviderChain(params)) {
      verifyAccessKeys(params, invalids);
    }

    final String credentialsType = getCredentialsType(params);
    if (isTempCredentialsOption(credentialsType)) {
      if (StringUtil.isEmptyOrSpaces(getIamRoleArnParam(params))) {
        invalids.put(IAM_ROLE_ARN_PARAM, IAM_ROLE_ARN_LABEL + " must not be empty");
      }
    } else if (StringUtil.isEmptyOrSpaces(credentialsType)) {
      invalids.put(CREDENTIALS_TYPE_PARAM, CREDENTIALS_TYPE_LABEL + " must not be empty");
    } else if (!isAccessKeysOption(credentialsType)) {
      invalids.put(CREDENTIALS_TYPE_PARAM, CREDENTIALS_TYPE_LABEL + " has unexpected value " + credentialsType);
    }

    if (ENVIRONMENT_TYPE_CUSTOM.equals(params.get(ENVIRONMENT_NAME_PARAM))) {
      verifyEndpoint(params, invalids);
    }

    return invalids;
  }

  public static void verifyEndpoint(@NotNull Map<String, String> params, Map<String, String> invalids) {
    final String serviceEndpoint = params.get(SERVICE_ENDPOINT_PARAM);
    if (StringUtil.isEmptyOrSpaces(serviceEndpoint)) {
      invalids.put(SERVICE_ENDPOINT_PARAM, SERVICE_ENDPOINT_LABEL + " must not be empty");
    } else {
      try {
        new URL(serviceEndpoint);
      } catch (MalformedURLException e) {
        invalids.put(SERVICE_ENDPOINT_PARAM, "Invalid URL format for " + SERVICE_ENDPOINT_LABEL);
      }
    }
  }

  public static void verifyAccessKeys(@NotNull Map<String, String> params, Map<String, String> invalids) {
    if (StringUtil.isEmptyOrSpaces(getAccessKeyId(params))) {
      invalids.put(ACCESS_KEY_ID_PARAM, ACCESS_KEY_ID_LABEL + " must not be empty");
    }
    if (StringUtil.isEmptyOrSpaces(getSecretAccessKey(params))) {
      invalids.put(SECURE_SECRET_ACCESS_KEY_PARAM, SECRET_ACCESS_KEY_LABEL + " must not be empty");
    }
  }

  private static boolean isAccessKeysOption(String credentialsType) {
    return ACCESS_KEYS_OPTION.equals(credentialsType) || ACCESS_KEYS_OPTION_OLD.equals(credentialsType);
  }

  private static boolean isTempCredentialsOption(String credentialsType) {
    return TEMP_CREDENTIALS_OPTION.equals(credentialsType) || TEMP_CREDENTIALS_OPTION_OLD.equals(credentialsType);
  }

  @Nullable
  private static String getIamRoleArnParam(@NotNull Map<String, String> params) {
    return getNewOrOld(params, IAM_ROLE_ARN_PARAM, IAM_ROLE_ARN_PARAM_OLD);
  }

  @Nullable
  public static String getCredentialsType(@NotNull Map<String, String> params) {
    return getNewOrOld(params, CREDENTIALS_TYPE_PARAM, CREDENTIALS_TYPE_PARAM_OLD);
  }

  @Nullable
  public static String getAccessKeyId(@NotNull Map<String, String> params) {
    return getNewOrOld(params, ACCESS_KEY_ID_PARAM, ACCESS_KEY_ID_PARAM_OLD);
  }

  @NotNull
  public static AwsCredentialsProvider getCredentialsProvider(@NotNull final Map<String, String> params){
    return getCredentialsProvider(params, false);
  }

  @NotNull
  private static AwsCredentialsProvider getCredentialsProvider(@NotNull final Map<String, String> params,
                                                               final boolean fixedCredentials){
    final String credentialsType = getCredentialsType(params);

    if (isAccessKeysOption(credentialsType) || fixedCredentials){
      if (isUseDefaultCredentialProviderChain(params)) {
        return DefaultCredentialsProvider.builder()
          .build();
      }

      return () -> AwsBasicCredentials.create(getAccessKeyId(params), getSecretAccessKey(params));
    }
    if (isTempCredentialsOption(credentialsType)) {
      return createSessionCredentialsProvider(params);
    }

    // a workaround to not return a DefaultAWSCredentialsProviderChain (null)
    // I'm afraid throwing an exception here could result in undesired behaviour in different places
    //TODO: remove this as well (throw an exception instead)
    return () -> AwsBasicCredentials.create("", "");
  }

  private static boolean isUseDefaultCredentialProviderChain(@NotNull Map<String, String> params) {
    if (TeamCityProperties.getBoolean(DEFAULT_CREDENTIALS_PROVIDER_CHAIN_DISABLED_PARAM)) {
      return false;
    }
    return Boolean.parseBoolean(params.get(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM)) || Boolean.parseBoolean(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM_OLD);
  }

  @Nullable
  private static String getExternalId(@NotNull Map<String, String> params) {
    return getNewOrOld(params, EXTERNAL_ID_PARAM, EXTERNAL_ID_PARAM_OLD);
  }

  @Nullable
  private static String getSecretAccessKey(@NotNull Map<String, String> params) {
    String secretAccessKeyParam = params.get(SECURE_SECRET_ACCESS_KEY_PARAM);
    if (StringUtil.isNotEmpty(secretAccessKeyParam)) return secretAccessKeyParam;

    secretAccessKeyParam = params.get(SECURE_SECRET_ACCESS_KEY_PARAM_OLD);
    return StringUtil.isNotEmpty(secretAccessKeyParam) ? secretAccessKeyParam : params.get(SECRET_ACCESS_KEY_PARAM_OLD);
  }

  /**
   * Default region name will be returned if neither new nor old parameter is specified.
   * @param params
   * @return region name
   */
  @NotNull
  public static String getRegionName(@NotNull Map<String, String> params) {
    String regionName = getNewOrOld(params, REGION_NAME_PARAM, REGION_NAME_PARAM_OLD);

    if (regionName == null) {
      Loggers.SERVER.debug("Region name is not specified, using default region name: " + AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
      return AwsCloudConnectorConstants.REGION_NAME_DEFAULT;
    }

    return regionName;
  }

  @Nullable
  private static String getNewOrOld(@NotNull Map<String, String> params, @NotNull String newKey, @NotNull String oldKey) {
    final String newVal = params.get(newKey);
    return StringUtil.isNotEmpty(newVal) ? newVal : params.get(oldKey);
  }

  @NotNull
  public static Map<String, String> getDefaults(@Nullable String serverUUID) {
    final Map<String, String> defaults = new HashMap<String, String>(DEFAULTS);
    if (StringUtil.isNotEmpty(serverUUID)) {
      defaults.put(EXTERNAL_ID_PARAM, "TeamCity-server-" + serverUUID);
    }
    return defaults;
  }

  private static boolean isReference(@NotNull String param, boolean acceptReferences) {
    return acceptReferences && ReferencesResolverUtil.containsReference(param);
  }

  static int patchSessionDuration(int sessionDuration) {
    if (sessionDuration < 900) return 900;
    if (sessionDuration > 3600) return 3600;
    return sessionDuration;
  }

  @NotNull
  static String patchSessionName(@NotNull String sessionName) {
    return StringUtil.truncateStringValue(sessionName.replaceAll(UNSUPPORTED_SESSION_NAME_CHARS, "_"), MAX_SESSION_NAME_LENGTH);
  }

  @NotNull
  static SdkHttpClient.Builder<ApacheHttpClient.Builder> createClientBuilder() {
    return createClientBuilderEx(null);
  }

  public static SdkHttpClient.Builder<ApacheHttpClient.Builder> createClientBuilderEx(@Nullable String suffix){
    return ClientConfigurationBuilder.createClientBuilder(suffix);
  }

  public static ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder() {
    return ClientConfigurationBuilder.clientOverrideConfigurationBuilder();
  }

  public interface WithAWSClients<T, E extends Throwable> {
    @Nullable T run(@NotNull AWSClients clients) throws E;
  }

  public static <T, E extends Throwable> T withAWSClients(@NotNull Map<String, String> params,
                                                          @NotNull WithAWSClients<T, E> withAWSClients) throws E {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(AWSCommonParams.class.getClassLoader());
    try {
      return withAWSClients.run(createAWSClients(params));
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  @NotNull
  private static AWSClients createAWSClients(@NotNull Map<String, String> params) {
    final String regionName = getRegionName(params);

    final String accessKeyId = getAccessKeyId(params);
    final String secretAccessKey = getSecretAccessKey(params);

    final AWSClients awsClients;
    if (isTempCredentialsOption(getCredentialsType(params))) {
      final String iamRoleARN = getIamRoleArnParam(params);
      final String externalID = getExternalId(params);
      final String sessionName = getStringOrDefault(params.get(TEMP_CREDENTIALS_SESSION_NAME_PARAM), TEMP_CREDENTIALS_SESSION_NAME_DEFAULT_PREFIX + new Date().getTime());
      final int sessionDuration = getIntegerOrDefault(params.get(TEMP_CREDENTIALS_DURATION_SEC_PARAM), TEMP_CREDENTIALS_DURATION_SEC_DEFAULT);

      awsClients = isUseDefaultCredentialProviderChain(params)
                   ? fromSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration, regionName)
                   : fromSessionCredentials(accessKeyId, secretAccessKey, iamRoleARN, externalID, sessionName, sessionDuration, regionName);
    } else {
      awsClients = isUseDefaultCredentialProviderChain(params) ?
                   fromDefaultCredentialProviderChain(regionName) :
                   fromBasicCredentials(accessKeyId, secretAccessKey, regionName);
    }

    final String environmentType = params.get(ENVIRONMENT_NAME_PARAM);
    if (StringUtil.areEqualIgnoringCase(ENVIRONMENT_TYPE_CUSTOM, environmentType)) {
      final String serviceEndpoint = params.get(SERVICE_ENDPOINT_PARAM);
      awsClients.setServiceEndpoint(serviceEndpoint);
    }

    return awsClients;
  }

  private static AwsCredentialsProvider createSessionCredentialsProvider(Map<String, String> params) throws AWSException {
    final String iamRoleARN = getIamRoleArnParam(params);
    final String externalID = getExternalId(params);
    final String sessionName = getStringOrDefault(params.get(TEMP_CREDENTIALS_SESSION_NAME_PARAM), TEMP_CREDENTIALS_SESSION_NAME_DEFAULT_PREFIX + new Date().getTime());
    final int sessionDuration = getIntegerOrDefault(params.get(TEMP_CREDENTIALS_DURATION_SEC_PARAM), TEMP_CREDENTIALS_DURATION_SEC_DEFAULT);

    try {
      if (StringUtil.isEmptyOrSpaces(iamRoleARN)){
        return StsGetSessionTokenCredentialsProvider.builder()
          .stsClient(createSecurityTokenService(params))
          .build();
      } else {
        AssumeRoleRequest.Builder reqBuilder = AssumeRoleRequest.builder()
          .roleArn(iamRoleARN)
          .roleSessionName(sessionName)
          .durationSeconds(sessionDuration);

        if (StringUtil.isNotEmpty(externalID)) {
          reqBuilder.externalId(externalID);
        }

        AssumeRoleRequest assumeRoleRequest = reqBuilder.build();

        StsAssumeRoleCredentialsProvider.Builder builder = StsAssumeRoleCredentialsProvider.builder()
          .refreshRequest(assumeRoleRequest)
          .stsClient(createSecurityTokenService(params));

        return builder.build();
      }
    } catch (Exception e) {
      throw new AWSException(e);
    }
  }

  @NotNull
  private static StsClient createSecurityTokenService(Map<String, String> params) {
    final String region = getRegionName(params);
    return StsClient.builder()
      .defaultsMode(DefaultsMode.STANDARD)
      .region(Region.of(region))
      .httpClientBuilder(createClientBuilderEx("sts"))
      .overrideConfiguration(clientOverrideConfigurationBuilder().build())
      .credentialsProvider(getCredentialsProvider(params, true))
      .build();
  }


  @NotNull
  public static String getStringOrDefault(@Nullable String val, @NotNull String defaultVal) {
    return StringUtil.isEmptyOrSpaces(val) ? defaultVal : val;
  }

  public static int getIntegerOrDefault(@Nullable String val, int defaultVal) {
    try {
      if (StringUtil.isNotEmpty(val)) return Integer.parseInt(val);
    } catch (NumberFormatException e) { /* see below */ }
    return defaultVal;
  }

  public static int calculateIdentity(@NotNull String baseDir, @NotNull Map<String, String> params, @NotNull Collection<String> otherParts) {
    return calculateIdentity(baseDir, params, CollectionsUtil.toStringArray(otherParts));
  }

  public static int calculateIdentity(@NotNull String baseDir, @NotNull Map<String, String> params, String... otherParts) {
    List<String> allParts = new ArrayList<String>(CollectionsUtil.join(getIdentityFormingParams(params), Arrays.asList(otherParts)));
    allParts = CollectionsUtil.filterNulls(allParts);
    Collections.sort(allParts);

    baseDir = FileUtil.toSystemIndependentName(baseDir);
    final StringBuilder sb = new StringBuilder();
    for (String p : allParts) {
      if (StringUtil.isEmptyOrSpaces(p)) continue;

      p = FileUtil.toSystemIndependentName(p);
      if (baseDir.length() > 0) {
        p = p.replace(baseDir, "");
      }
      sb.append(p);
    }

    return sb.toString().replace(" ", "").toLowerCase().hashCode();
  }

  @NotNull
  private static Collection<String> getIdentityFormingParams(@NotNull Map<String, String> params) {
    return Arrays.asList(getRegionName(params), getAccessKeyId(params), getIamRoleArnParam(params));
  }

  // must implement AWSSessionCredentials as AWS SDK may use "instanceof"
  static abstract class LazyCredentials implements AwsSessionCredentialsIdentity, AwsCredentials {
    @Nullable
    private AwsSessionCredentials myDelegate = null;

    @Override
    public String accessKeyId() {
      return getDelegate().accessKeyId();
    }

    @Override
    public String secretAccessKey() {
      return getDelegate().secretAccessKey();
    }

    @Override
    public String sessionToken() {
      return getDelegate().sessionToken();
    }

    @NotNull
    private AwsSessionCredentials getDelegate() {
      if (myDelegate == null) myDelegate = createCredentials();
      return myDelegate;
    }

    @NotNull
    protected abstract AwsSessionCredentials createCredentials();
  }
}