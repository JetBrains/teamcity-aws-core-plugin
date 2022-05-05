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

package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import jetbrains.buildServer.clouds.amazon.connector.AwsCredentials;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StaticCredentialsHolder implements AwsCredentialsHolder {

  private final String accessKeyId;
  private final String secretAccessKey;

  public StaticCredentialsHolder(String accessKeyId, String secretAccessKey) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
  }


  @NotNull
  @Override
  public AwsCredentials getAwsCredentials() {
    return new AwsCredentials() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return accessKeyId;
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return secretAccessKey;
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return null;
      }
    };
  }

  @Override
  public void refreshCredentials() {
    //...
  }
}
