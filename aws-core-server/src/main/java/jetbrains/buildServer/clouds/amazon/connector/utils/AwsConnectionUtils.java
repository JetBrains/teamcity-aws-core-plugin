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

package jetbrains.buildServer.clouds.amazon.connector.utils;

import com.amazonaws.auth.*;
import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsConnectionUtils {
  @NotNull
  public static AWSCredentialsProvider awsCredsProviderFromData(@NotNull final AwsCredentialsData credentialsData) {
    AWSCredentials credentials;
    if (credentialsData.getSessionToken() == null) {
      credentials = new BasicAWSCredentials(
        credentialsData.getAccessKeyId(),
        credentialsData.getSecretAccessKey()
      );
    } else {
      credentials = new BasicSessionCredentials(
        credentialsData.getAccessKeyId(),
        credentialsData.getSecretAccessKey(),
        credentialsData.getSessionToken()
      );
    }

    return new AWSStaticCredentialsProvider(credentials);
  }

  @NotNull
  public static AwsCredentialsData getDataFromCredentials(@NotNull final Credentials credentials){
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getSecretAccessKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        return credentials.getSessionToken();
      }
    };
  }
}