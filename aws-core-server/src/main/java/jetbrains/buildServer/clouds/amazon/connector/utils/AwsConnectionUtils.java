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
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionUtils {
  @NotNull
  public static AWSCredentialsProvider awsCredsProviderFromHolder(@NotNull final AwsCredentialsHolder credentialsHolder) {
    AWSCredentials credentials;
    if (credentialsHolder.getAwsCredentials().getSessionToken() == null) {
      credentials = new BasicAWSCredentials(
        credentialsHolder.getAwsCredentials().getAccessKeyId(),
        credentialsHolder.getAwsCredentials().getSecretAccessKey()
      );
    } else {
      credentials = new BasicSessionCredentials(
        credentialsHolder.getAwsCredentials().getAccessKeyId(),
        credentialsHolder.getAwsCredentials().getSecretAccessKey(),
        credentialsHolder.getAwsCredentials().getSessionToken()
      );
    }

    return new AWSStaticCredentialsProvider(credentials);
  }
}