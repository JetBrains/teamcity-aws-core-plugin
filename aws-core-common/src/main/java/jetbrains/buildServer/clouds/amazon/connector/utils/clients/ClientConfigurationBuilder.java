package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.version.ServerVersionHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.TeamCityProperties.getInteger;
import static jetbrains.buildServer.serverSide.TeamCityProperties.getPropertyOrNull;

public class ClientConfigurationBuilder {
  public static ClientConfiguration createClientConfigurationEx(@Nullable String suffix){
    if (StringUtil.isEmpty(suffix)){
      suffix = AwsCloudConnectorConstants.DEFAULT_SUFFIX;
    }
    final ClientConfiguration config = new ClientConfiguration();

    int connectionTimeout = TeamCityProperties.getInteger(String.format("teamcity.%s.timeout", suffix), AwsCloudConnectorConstants.DEFAULT_CONNECTION_TIMEOUT);
    config.setConnectionTimeout(connectionTimeout);
    config.setSocketTimeout(connectionTimeout);
    // version copy-pasted from jetbrains.buildServer.updates.VersionChecker.retrieveUpdates()
    config.setUserAgentPrefix("TeamCity Server " + ServerVersionHolder.getVersion().getDisplayVersion() + " (build " + ServerVersionHolder.getVersion().getBuildNumber() + ")");
    config.setProtocol(Protocol.HTTPS);

    final String PREFIX = "teamcity.http.proxy.";

    config.setProxyHost(getPropertyEx(PREFIX + "host", suffix, config.getProxyHost()));
    config.setProxyPort(getIntegerEx(PREFIX + "port", suffix, config.getProxyPort()));
    config.setProxyDomain(getPropertyEx(PREFIX + "domain", suffix, config.getProxyDomain()));
    config.setProxyUsername(getPropertyEx(PREFIX + "user", suffix, config.getProxyUsername()));
    config.setProxyPassword(getPropertyEx(PREFIX + "password", suffix, config.getProxyPassword()));
    config.setProxyWorkstation(getPropertyEx(PREFIX + "workstation", suffix, config.getProxyWorkstation()));
    return config;
  }

  private static String getPropertyEx(@NotNull String baseName, @NotNull String suffix, @Nullable String defaultValue){
    final String propertyOrNull = getPropertyOrNull(baseName + "." + suffix, getPropertyOrNull(baseName + "." + AwsCloudConnectorConstants.DEFAULT_SUFFIX));

    return propertyOrNull == null ? defaultValue : propertyOrNull;
  }

  private static Integer getIntegerEx(@NotNull String baseName, @NotNull String suffix, int defaultValue){
    final int intValue = getInteger(baseName + "."+ suffix, getInteger(baseName + "." + AwsCloudConnectorConstants.DEFAULT_SUFFIX));

    return intValue == 0 ? defaultValue : intValue;

  }
}
