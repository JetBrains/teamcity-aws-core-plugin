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

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public abstract class BaseAwsCredentialsBuilder implements AwsCredentialsBuilder {

  @Override
  @NotNull
  public AwsCredentialsHolder constructSpecificCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException {
    List<InvalidProperty> invalidProperties = validateProperties(cloudConnectorProperties);
    if (! invalidProperties.isEmpty()) {
      InvalidProperty lastInvalidProperty = invalidProperties.get(invalidProperties.size() - 1);
      String errorDescription = StringUtil.emptyIfNull(lastInvalidProperty.getInvalidReason());
      throw new AwsConnectorException(
        errorDescription,
        lastInvalidProperty.getPropertyName()
      );
    }
    return constructConcreteCredentialsProviderImpl(cloudConnectorProperties);
  }

  @NotNull
  protected abstract AwsCredentialsHolder constructConcreteCredentialsProviderImpl(@NotNull final Map<String, String> cloudConnectorProperties) throws AwsConnectorException;
}
