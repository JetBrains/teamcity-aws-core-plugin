/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class AwsConnectionParameters {
  private final String myAwsConnectionId;
  private final String myInternalProjectId;
  private final String mySessionDuration;

  private AwsConnectionParameters(@Nullable String awsConnectionId, @Nullable String internalProjectId,
                                 @NotNull String sessionDuration) {
    myAwsConnectionId = awsConnectionId;
    myInternalProjectId = internalProjectId;
    mySessionDuration = sessionDuration;
  }

  @Nullable
  public String getAwsConnectionId() {
    return myAwsConnectionId;
  }

  @Nullable
  public String getInternalProjectId() {
    return myInternalProjectId;
  }

  @NotNull
  public String getSessionDuration() {
    return mySessionDuration;
  }

  public boolean hasConnectionIdentity() {
    return !StringUtil.isEmptyOrSpaces(myAwsConnectionId) && !StringUtil.isEmptyOrSpaces(myInternalProjectId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsConnectionParameters that = (AwsConnectionParameters) o;
    return Objects.equals(myAwsConnectionId, that.myAwsConnectionId)
      && Objects.equals(myInternalProjectId, that.myInternalProjectId)
      && Objects.equals(mySessionDuration, that.mySessionDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myAwsConnectionId, myInternalProjectId, mySessionDuration);
  }

  @Override
  public String toString() {
    return "AwsConnectionParameters{" +
      "myAwsConnectionId='" + myAwsConnectionId + '\'' +
      ", myInternalProjectId='" + myInternalProjectId + '\'' +
      ", mySessionDuration='" + mySessionDuration + '\'' +
      '}';
  }

  public static class AwsConnectionParametersBuilder {
    private final String myAwsConnectionId;
    private String myInternalProjectId;
    private String mySessionDuration = AwsSessionCredentialsParams.SESSION_DURATION_DEFAULT;

    private AwsConnectionParametersBuilder(@Nullable String awsConnectionId) {
      myAwsConnectionId = awsConnectionId;
    }

    @NotNull
    public static AwsConnectionParametersBuilder of(@Nullable String awsConnectionId) {
      return new AwsConnectionParametersBuilder(awsConnectionId);
    }

    @NotNull
    public AwsConnectionParametersBuilder withInternalProjectId(@Nullable String internalProjectId) {
      myInternalProjectId = internalProjectId;
      return this;
    }

    public AwsConnectionParametersBuilder withSessionDuration(@Nullable String sessionDuration) {
      mySessionDuration = sessionDuration;
      return this;
    }

    @NotNull
    public AwsConnectionParameters build() {
      return new AwsConnectionParameters(myAwsConnectionId, myInternalProjectId, mySessionDuration);
    }
  }
}
