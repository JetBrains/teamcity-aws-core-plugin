

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