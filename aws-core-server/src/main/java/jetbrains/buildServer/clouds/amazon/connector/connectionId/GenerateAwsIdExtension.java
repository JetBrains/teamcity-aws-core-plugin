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

package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.controllers.admin.projects.GenerateExternalIdExtension;
import jetbrains.buildServer.serverSide.identifiers.ExternalIdGenerator;
import org.jetbrains.annotations.NotNull;

public class GenerateAwsIdExtension implements GenerateExternalIdExtension {

  private final AwsConnectionIdGenerator myAwsConnectionIdGenerator;

  public GenerateAwsIdExtension(@NotNull final AwsConnectionIdGenerator awsConnectionIdGenerator) {
    myAwsConnectionIdGenerator = awsConnectionIdGenerator;
  }

  @NotNull
  @Override
  public String getObjectId() {
    return AwsCloudConnectorConstants.AWS_CONNECTION_ID_GENERATOR_TYPE;
  }

  @NotNull
  @Override
  public ExternalIdGenerator getIdentifiersGenerator() {
    return myAwsConnectionIdGenerator;
  }
}
