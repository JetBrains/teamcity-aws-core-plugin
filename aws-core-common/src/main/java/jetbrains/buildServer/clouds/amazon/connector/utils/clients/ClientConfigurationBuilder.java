package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.version.ServerVersionHolder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

import java.net.URI;
import java.time.Duration;

import static jetbrains.buildServer.serverSide.TeamCityProperties.getInteger;
import static jetbrains.buildServer.serverSide.TeamCityProperties.getPropertyOrNull;

public class ClientConfigurationBuilder {

  public static ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder() {
    String userAgentPrefix = "TeamCity Server " + ServerVersionHolder.getVersion().getDisplayVersion()
      + " (build " + ServerVersionHolder.getVersion().getBuildNumber() + ")";

    return ClientOverrideConfiguration.builder()
      .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, userAgentPrefix);
  }

  public static SdkHttpClient.Builder<ApacheHttpClient.Builder> createClientBuilder(@Nullable String suffix,
                                                                                    @Nullable ConnectionSocketFactory socketFactory) {
    if (StringUtil.isEmpty(suffix)) {
      suffix = AwsCloudConnectorConstants.DEFAULT_SUFFIX;
    }

    final String PREFIX = "teamcity.http.proxy.";

    String host = getPropertyEx(PREFIX + "host", suffix);
    int port = getIntegerEx(PREFIX + "port", suffix);
    URI proxyEndpoint = null;
    if (StringUtil.isNotEmpty(host)) {
      if (port > 0) {
        proxyEndpoint = URI.create("http://" + host + ":" + port);
      } else {
        proxyEndpoint = URI.create("http://" + host);
      }
    }

    ProxyConfiguration.Builder proxyConfigurationBuilder = ProxyConfiguration.builder()
      .username(getPropertyEx(PREFIX + "user", suffix))
      .password(getPropertyEx(PREFIX + "password", suffix))
      .ntlmWorkstation(getPropertyEx(PREFIX + "workstation", suffix))
      .ntlmDomain(getPropertyEx(PREFIX + "domain", suffix));

    if (proxyEndpoint != null) {
      proxyConfigurationBuilder.endpoint(proxyEndpoint);
    }

    int timeout = TeamCityProperties.getInteger(String.format("teamcity.%s.timeout", suffix), AwsCloudConnectorConstants.DEFAULT_CONNECTION_TIMEOUT);
    Duration timeoutDuration = Duration.ofMillis(timeout);

    return ApacheHttpClient.builder()
      .connectionTimeout(timeoutDuration)
      .socketTimeout(timeoutDuration)
      .proxyConfiguration(proxyConfigurationBuilder.build())
      .socketFactory(socketFactory);
  }

  public static SdkHttpClient.Builder<ApacheHttpClient.Builder> createClientBuilder(){
    return createClientBuilder(null, null);
  }

  public static SdkHttpClient.Builder<ApacheHttpClient.Builder> createClientBuilder(@Nullable String suffix){
    return createClientBuilder(suffix, null);
  }

  public static SdkAsyncHttpClient.Builder<NettyNioAsyncHttpClient.Builder> createAsyncClientBuilder(@Nullable String suffix, @Nullable TlsTrustManagersProvider tlsTrustManagersProvider) {
    if (StringUtil.isEmpty(suffix)) {
      suffix = AwsCloudConnectorConstants.DEFAULT_SUFFIX;
    }

    int timeout = TeamCityProperties.getInteger(String.format("teamcity.%s.timeout", suffix), AwsCloudConnectorConstants.DEFAULT_CONNECTION_TIMEOUT);
    Duration timeoutDuration = Duration.ofMillis(timeout);

    final String PREFIX = "teamcity.http.proxy.";

    software.amazon.awssdk.http.nio.netty.ProxyConfiguration proxyConfiguration = software.amazon.awssdk.http.nio.netty.ProxyConfiguration.builder()
      .username(getPropertyEx(PREFIX + "user", suffix))
      .password(getPropertyEx(PREFIX + "password", suffix))
      .host(getPropertyEx(PREFIX + "host", suffix))
      .build();

    NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder()
      .proxyConfiguration(proxyConfiguration)
      .connectionTimeout(timeoutDuration)
      .readTimeout(timeoutDuration);

    if (tlsTrustManagersProvider != null) {
      builder.tlsTrustManagersProvider(tlsTrustManagersProvider);
    }

    return builder;
  }

  private static String getPropertyEx(@NotNull String baseName, @NotNull String suffix){
    return getPropertyOrNull(baseName + "." + suffix, getPropertyOrNull(baseName + "." + AwsCloudConnectorConstants.DEFAULT_SUFFIX));
  }

  private static int getIntegerEx(@NotNull String baseName, @NotNull String suffix){
    return getInteger(baseName + "."+ suffix, getInteger(baseName + "." + AwsCloudConnectorConstants.DEFAULT_SUFFIX));
  }
}
