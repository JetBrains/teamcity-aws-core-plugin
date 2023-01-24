package jetbrains.buildServer.clouds.amazon.connector.common.impl;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionDescriptor;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.ConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionDescriptorImpl implements AwsConnectionDescriptor {

  private final AwsCredentialsHolder myAwsCredentialsHolder;
  private final SProjectFeatureDescriptor myProjectFeatureDescriptor;
  private final ConnectionProvider myAwsConnectionProvider;

  private final String myDescription;
  private final boolean myIsUsingSessionCredentials;

  public AwsConnectionDescriptorImpl(@NotNull final SProjectFeatureDescriptor projectFeatureDescriptor,
                                     @NotNull final AwsCredentialsHolder awsCredentialsHolder,
                                     @NotNull final ConnectionProvider awsConnectionProvider,
                                     @NotNull final String description) {
    myAwsCredentialsHolder = awsCredentialsHolder;
    myProjectFeatureDescriptor = projectFeatureDescriptor;
    myAwsConnectionProvider = awsConnectionProvider;
    myDescription = description;
    myIsUsingSessionCredentials = awsCredentialsHolder.getAwsCredentials().getSessionToken() != null;
  }

  @NotNull
  @Override
  public AwsCredentialsHolder getAwsCredentialsHolder() {
    return myAwsCredentialsHolder;
  }

  @NotNull
  @Override
  public String getDescription() {
    return myDescription;
  }

  @NotNull
  @Override
  public String getRegion() {
    return getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM);
  }

  @Override
  public boolean isUsingSessionCredentials() {
    return myIsUsingSessionCredentials;
  }

  @NotNull
  @Override
  public String getType() {
    return myProjectFeatureDescriptor.getType();
  }

  @NotNull
  @Override
  public String getConnectionDisplayName() {
    return StringUtil.emptyIfNull(myProjectFeatureDescriptor.getParameters().get(OAuthConstants.DISPLAY_NAME_PARAM));
  }

  @NotNull
  @Override
  public ConnectionProvider getConnectionProvider() {
    return myAwsConnectionProvider;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    return myProjectFeatureDescriptor.getParameters();
  }

  @NotNull
  @Override
  public String getProjectId() {
    return myProjectFeatureDescriptor.getProjectId();
  }

  @NotNull
  @Override
  public String getId() {
    return myProjectFeatureDescriptor.getId();
  }

  @NotNull
  @Override
  public AwsCredentialsData getConnectionCredentials() {
    return myAwsCredentialsHolder.getAwsCredentials();
  }
}
