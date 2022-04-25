package jetbrains.buildServer.clouds.amazon.connector.backwardsCompat;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.AwsConnectionsManager;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars.AwsConnToEnvVarsBuildFeature;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToEnvVars.InjectAwsConnDataToEnvVars;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

public class AwsConnectorExtensionRegistar {

  private final ExtensionHolder myExtensionHolder;
  private final PluginDescriptor myPluginDescriptor;
  private final AwsConnectorFactory myAwsConnectorFactory;
  private final AwsConnectionsManager myAwsConnectionsManager;

  public AwsConnectorExtensionRegistar(@NotNull final ExtensionHolder extensionHolder,
                                       @NotNull final PluginDescriptor pluginDescriptor,
                                       @NotNull final AwsConnectorFactory awsConnectorFactory,
                                       @NotNull final AwsConnectionsManager awsConnectionsManager) {
    myExtensionHolder = extensionHolder;
    myPluginDescriptor = pluginDescriptor;
    myAwsConnectorFactory = awsConnectorFactory;
    myAwsConnectionsManager = awsConnectionsManager;

    if (TeamCityProperties.getBoolean(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME)) {

      registerAwsConnection();
      registerExposeToEnvVarsBuildFeature();

    } else {
      Loggers.CLOUD.debug("AWS Core plugin has not been loaded since the is no definied internal property or its turned off: " + AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME);
    }
  }

  private void registerAwsConnection() {
    myExtensionHolder.registerExtension(OAuthProvider.class, AwsConnectionProvider.class.getName(), new AwsConnectionProvider(myPluginDescriptor, myAwsConnectorFactory));
  }

  private void registerExposeToEnvVarsBuildFeature() {
    myExtensionHolder.registerExtension(BuildFeature.class, AwsConnToEnvVarsBuildFeature.class.getName(), new AwsConnToEnvVarsBuildFeature(myPluginDescriptor));

    InjectAwsConnDataToEnvVars awsConnDataToEnvVars = new InjectAwsConnDataToEnvVars(myAwsConnectionsManager);
    myExtensionHolder.registerExtension(BuildStartContextProcessor.class, InjectAwsConnDataToEnvVars.class.getName(), awsConnDataToEnvVars);
    myExtensionHolder.registerExtension(PasswordsProvider.class, InjectAwsConnDataToEnvVars.class.getName(), awsConnDataToEnvVars);
  }
}
