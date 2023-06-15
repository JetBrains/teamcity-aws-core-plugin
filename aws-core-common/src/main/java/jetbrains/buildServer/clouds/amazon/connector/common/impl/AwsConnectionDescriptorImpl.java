package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.serverSide.connections.utils.ConnectionUtils;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionDescriptorImpl extends ProjectFeatureDescriptorImpl implements AwsConnectionDescriptor {

  private final AwsCredentialsHolder myAwsCredentialsHolder;
  private final ConnectionProvider myConnectionProvider;

  public AwsConnectionDescriptorImpl(@NotNull final SProjectFeatureDescriptor projectFeatureDescriptor,
                                     @NotNull final AwsCredentialsHolder awsCredentialsHolder,
                                     @NotNull final ExtensionHolder extensionHolder) {
    super(
      projectFeatureDescriptor.getId(),
      projectFeatureDescriptor.getType(),
      projectFeatureDescriptor.getParameters(),
      projectFeatureDescriptor.getProjectId()
    );
    myAwsCredentialsHolder = awsCredentialsHolder;

    myConnectionProvider = ConnectionUtils.getConnectionProviderOfType(extensionHolder, AwsCloudConnectorConstants.CLOUD_TYPE);
  }

  @NotNull
  @Override
  public AwsCredentialsHolder getAwsCredentialsHolder() {
    return myAwsCredentialsHolder;
  }

  @NotNull
  @Override
  public String getRegion() {
    return getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return StringUtil.emptyIfNull(getParameters().get(OAuthConstants.DISPLAY_NAME_PARAM));
  }

  @NotNull
  @Override
  public ConnectionProvider getConnectionProvider() {
    return myConnectionProvider;
  }
}
