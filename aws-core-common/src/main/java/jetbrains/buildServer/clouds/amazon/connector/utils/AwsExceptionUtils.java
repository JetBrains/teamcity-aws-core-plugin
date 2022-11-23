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

import com.amazonaws.AmazonServiceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AwsExceptionUtils {
  @Nullable
  public static String getAwsErrorMessage(@NotNull final Throwable exception){
    Throwable cause = exception.getCause();

    try {
      if (isAmazonServiceException(exception)) {
        return String.format(
          "Error type: <%s>, message: %s",
          ((AmazonServiceException)exception).getErrorType(),
          ((AmazonServiceException)exception).getErrorMessage()
        );
      } else if (cause != null && isAmazonServiceException(cause)) {
        return String.format(
          "Error type: <%s>, message: %s",
          ((AmazonServiceException)cause).getErrorType(),
          ((AmazonServiceException)cause).getErrorMessage()
        );
      } else {
        return exception.getMessage();
      }

    } catch (ClassCastException classCastException) {
      return exception.getMessage();
    }
  }

  public static boolean isAmazonServiceException(@Nullable final Throwable e){
    if(e == null)
      return false;
    return AmazonServiceException.class.isAssignableFrom(e.getClass());
  }
}
