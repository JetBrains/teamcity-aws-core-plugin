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

package jetbrains.buildServer.clouds.amazon.connector.featureDevelopment;

import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionsManagerProvider {

  private final AwsConnectionsManager myAwsConnectionsManager;

  public AwsConnectionsManagerProvider(@NotNull final OAuthConnectionsManager connectionsManager,
                                       @NotNull final AwsConnectorFactory awsConnectorFactory) {
    myAwsConnectionsManager = new AwsConnectionsManager(connectionsManager, awsConnectorFactory);
  }

  @NotNull
  public AwsConnectionsManager getManager(){
    return myAwsConnectionsManager;
  }
}
