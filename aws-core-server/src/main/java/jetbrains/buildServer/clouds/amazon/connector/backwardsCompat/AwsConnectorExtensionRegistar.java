package jetbrains.buildServer.clouds.amazon.connector.backwardsCompat;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars.AwsConnToEnvVarsBuildFeature;
import jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars.InjectAwsConnDataToEnvVars;
import jetbrains.buildServer.clouds.amazon.connector.buildFeatures.envVars.InjectSecretAwsConnDataToEnvVars;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

public class AwsConnectorExtensionRegistar {
  public AwsConnectorExtensionRegistar(@NotNull final ExtensionHolder extensionHolder,
                                       @NotNull final PluginDescriptor pluginDescriptor,
                                       @NotNull final AwsConnectorFactory awsConnectorFactory,
                                       @NotNull final OAuthConnectionsManager oAuthConnectionsManager) {
    if (TeamCityProperties.getBoolean(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME)) {
      extensionHolder.registerExtension(OAuthProvider.class, AwsConnectionProvider.class.getName(), new AwsConnectionProvider(pluginDescriptor, awsConnectorFactory));
      extensionHolder.registerExtension(BuildFeature.class, AwsConnToEnvVarsBuildFeature.class.getName(), new AwsConnToEnvVarsBuildFeature(pluginDescriptor));
      extensionHolder.registerExtension(BuildStartContextProcessor.class, InjectAwsConnDataToEnvVars.class.getName(), new InjectAwsConnDataToEnvVars(oAuthConnectionsManager, awsConnectorFactory));
      extensionHolder.registerExtension(PasswordsProvider.class, InjectSecretAwsConnDataToEnvVars.class.getName(), new InjectSecretAwsConnDataToEnvVars(oAuthConnectionsManager, awsConnectorFactory));
    }
  }
}
