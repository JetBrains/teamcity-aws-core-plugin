/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package jetbrains.buildServer.clouds.amazon.connector.common;

import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans.AwsConnectionBean;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * All AWS Connections-related management logic is in the {@link jetbrains.buildServer.serverSide.connections.aws.AwsConnectionCredentialsFactory}.
 * @deprecated Use {@link jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager} to retrieve credentials.
 */
@Deprecated
public interface AwsConnectionDescriptorBuilder {
  @NotNull
  AwsConnectionDescriptor buildFromProject(@NotNull final SProject project, @NotNull final String awsFeatureConnectionId) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor fromFeatureDescriptor(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException;

  @NotNull
  AwsConnectionDescriptor buildWithSessionDuration(@NotNull final AwsConnectionDescriptor featureDescriptor, @NotNull final String sessionDuration) throws AwsConnectorException;

  @NotNull
  @Deprecated
  AwsConnectionBean awsConnBeanFromDescriptor(@NotNull final AwsConnectionDescriptor connectionDescriptor, @NotNull final Map<String, String> connectionParameters) throws AwsConnectorException;
}
