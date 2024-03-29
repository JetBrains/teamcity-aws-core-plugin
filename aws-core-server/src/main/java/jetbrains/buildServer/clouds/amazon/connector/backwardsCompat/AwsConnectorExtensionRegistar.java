package jetbrains.buildServer.clouds.amazon.connector.backwardsCompat;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.LinkedAwsConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.AwsConnectionIdGenerator;
import jetbrains.buildServer.clouds.amazon.connector.connectionId.GenerateAwsIdExtension;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent.AwsConnToAgentBuildFeature;
import jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent.InjectAwsCredentialsToTheBuildContext;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.admin.projects.GenerateExternalIdExtension;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

public class AwsConnectorExtensionRegistar {

  private final ExtensionHolder myExtensionHolder;
  private final PluginDescriptor myPluginDescriptor;
  private final AwsConnectionCredentialsFactory myAwsCredentialsFactory;
  private final LinkedAwsConnectionProvider myLinkedAwsConnectionProvider;
  private final AwsConnectionIdGenerator myAwsConnectionIdGenerator;

  public AwsConnectorExtensionRegistar(@NotNull final ExtensionHolder extensionHolder,
                                       @NotNull final PluginDescriptor pluginDescriptor,
                                       @NotNull final AwsConnectionCredentialsFactory awsCredentialsFactory,
                                       @NotNull final LinkedAwsConnectionProvider linkedAwsConnectionProvider,
                                       @NotNull final AwsConnectionIdGenerator awsConnectionIdGenerator) {
    myExtensionHolder = extensionHolder;
    myPluginDescriptor = pluginDescriptor;
    myAwsCredentialsFactory = awsCredentialsFactory;
    myLinkedAwsConnectionProvider = linkedAwsConnectionProvider;
    myAwsConnectionIdGenerator = awsConnectionIdGenerator;

    if (TeamCityProperties.getBooleanOrTrue(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME)) {

      registerAwsConnection();
      registerExposeToEnvVarsBuildFeature();

    } else {
      Loggers.CLOUD.debug("AWS Core plugin has not been loaded since the is no defined internal property or its turned off: " + AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME);
    }
  }

  private void registerAwsConnection() {
    AwsConnectionProvider awsConnectionProvider = new AwsConnectionProvider(myPluginDescriptor, myAwsCredentialsFactory);
    myExtensionHolder.registerExtension(ConnectionProvider.class, AwsConnectionProvider.class.getName(), awsConnectionProvider);
    myExtensionHolder.registerExtension(OAuthProvider.class, AwsConnectionProvider.class.getName(), awsConnectionProvider);

    GenerateAwsIdExtension generateAwsIdExtension = new GenerateAwsIdExtension(myAwsConnectionIdGenerator);
    myExtensionHolder.registerExtension(GenerateExternalIdExtension.class, GenerateAwsIdExtension.class.getName(), generateAwsIdExtension);
  }

  private void registerExposeToEnvVarsBuildFeature() {
    myExtensionHolder.registerExtension(BuildFeature.class, AwsConnToAgentBuildFeature.class.getName(), new AwsConnToAgentBuildFeature(myPluginDescriptor));

    InjectAwsCredentialsToTheBuildContext awsConnDataToEnvVars = new InjectAwsCredentialsToTheBuildContext(myLinkedAwsConnectionProvider);
    myExtensionHolder.registerExtension(BuildStartContextProcessor.class, InjectAwsCredentialsToTheBuildContext.class.getName(), awsConnDataToEnvVars);
  }
}
