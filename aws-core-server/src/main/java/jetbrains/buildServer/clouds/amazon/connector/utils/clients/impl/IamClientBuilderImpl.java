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

package jetbrains.buildServer.clouds.amazon.connector.utils.clients.impl;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.ClientConfigurationBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.clients.IamClientBuilder;
import org.jetbrains.annotations.NotNull;

public class IamClientBuilderImpl implements IamClientBuilder {
  @NotNull
  @Override
  public AmazonIdentityManagement createIamClient(@NotNull String connectionRegion, @NotNull AWSCredentialsProvider credentials) {
    return AmazonIdentityManagementClientBuilder
      .standard()
      .withRegion(Regions.fromName(connectionRegion))
      .withCredentials(credentials)
      .withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("iam"))
      .build();
  }
}
