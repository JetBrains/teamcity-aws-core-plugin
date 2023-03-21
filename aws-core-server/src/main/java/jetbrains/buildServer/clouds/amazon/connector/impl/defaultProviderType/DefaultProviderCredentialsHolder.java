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

package jetbrains.buildServer.clouds.amazon.connector.impl.defaultProviderType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultProviderCredentialsHolder implements AwsCredentialsHolder {

  private final SProjectFeatureDescriptor connectionFeatureDescriptor;

  private AWSCredentials credentials;
  private Date sessionExpirationDate;
  private final int MINIMAL_SESSION_DURATION_MINUTES = 15;

  public DefaultProviderCredentialsHolder(@NotNull final SProjectFeatureDescriptor featureDescriptor) throws AwsConnectorException {
    connectionFeatureDescriptor = featureDescriptor;
    constructNewDefaultProviderCredentials();
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAWSAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getAWSSecretKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        if (isUsingSessionCredentials()) {
          return ((AWSSessionCredentials)credentials).getSessionToken();
        } else {
          return null;
        }
      }
    };
  }

  @Override
  public void refreshCredentials() {
    try {
      constructNewDefaultProviderCredentials();
    } catch (AwsConnectorException e) {
      Loggers.CLOUD.warnAndDebugDetails(String.format(
              "Failed to refresh AWS Credentials using Default Credentials Provider Chain for Connection with ID: %s in the project with ID: %s, reason: %s",
              connectionFeatureDescriptor.getId(),
              connectionFeatureDescriptor.getProjectId(),
              e.getMessage()
      ), e);
    }
  }

  @Nullable
  @Override
  public Date getSessionExpirationDate() {
    if (sessionExpirationDate == null && isUsingSessionCredentials()) {
      refreshCredentials();
    }
    return sessionExpirationDate;
  }

  private void constructNewDefaultProviderCredentials() throws AwsConnectorException {
    try {
      credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
      if (isUsingSessionCredentials()) {
        Loggers.CLOUD.debug(String.format(
                "The Default Credentials Provider Chain uses session credentials, Connection ID: %s, project ID: %s",
                connectionFeatureDescriptor.getId(),
                connectionFeatureDescriptor.getProjectId()
        ));
        sessionExpirationDate = Date.from(Instant.now().plus(MINIMAL_SESSION_DURATION_MINUTES, ChronoUnit.MINUTES));
      } else {
        sessionExpirationDate = null;
      }
    } catch (Exception e) {
      throw new AwsConnectorException(String.format(
              "Failed to use the DefaultAWSCredentialsProviderChain, Connection ID: %s, project ID: %s, reason %s",
              connectionFeatureDescriptor.getId(),
              connectionFeatureDescriptor.getProjectId(),
              e.getMessage()
      ), e);
    }
  }

  private boolean isUsingSessionCredentials() {
    return credentials instanceof AWSSessionCredentials;
  }
}
