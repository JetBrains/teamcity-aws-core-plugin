/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.util.List;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
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
