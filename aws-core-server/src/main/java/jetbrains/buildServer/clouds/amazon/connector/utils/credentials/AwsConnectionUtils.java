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

package jetbrains.buildServer.clouds.amazon.connector.utils.credentials;

import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionUtils {
  @NotNull
  public static AwsConnectionBean awsConnBeanFromDescriptor(@NotNull final OAuthConnectionDescriptor connectionDescriptor, @NotNull final AwsConnectorFactory awsConnectorFactory) throws AwsConnectorException {
    AwsCredentialsHolder credentialsHolder = awsConnectorFactory.buildAwsCredentialsProvider(connectionDescriptor.getParameters());
    return new AwsConnectionBean(
      connectionDescriptor.getId(),
      connectionDescriptor.getDescription(),
      credentialsHolder,
      connectionDescriptor.getParameters().get(AwsCloudConnectorConstants.REGION_NAME_PARAM),
      ParamUtil.useSessionCredentials(connectionDescriptor.getParameters())
    );
  }
}
