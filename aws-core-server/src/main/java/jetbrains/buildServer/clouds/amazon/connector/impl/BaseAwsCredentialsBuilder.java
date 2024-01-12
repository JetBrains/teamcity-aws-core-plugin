

package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.List;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.IOGuard;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.connector.utils.AwsExceptionUtils.isAmazonServiceException;

public abstract class BaseAwsCredentialsBuilder implements AwsCredentialsBuilder {

  @Override
  @NotNull
  public AwsCredentialsHolder constructSpecificCredentialsProvider(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    List<InvalidProperty> invalidProperties = validateProperties(featureDescriptor.getParameters());
    if (!invalidProperties.isEmpty()) {
      InvalidProperty lastInvalidProperty = invalidProperties.get(invalidProperties.size() - 1);
      String errorDescription = StringUtil.emptyIfNull(lastInvalidProperty.getInvalidReason());
      throw new AwsConnectorException(
        errorDescription,
        lastInvalidProperty.getPropertyName()
      );
    }
    try {
      return IOGuard.allowNetworkCall(() -> constructSpecificCredentialsProviderImpl(featureDescriptor));
    } catch (Exception e) {
      if (isAmazonServiceException(e)) {
        throw new AwsConnectorException(e);
      }
      throw e;
    }
  }

  @NotNull
  protected abstract AwsCredentialsHolder constructSpecificCredentialsProviderImpl(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;
}