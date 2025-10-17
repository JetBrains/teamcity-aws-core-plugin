

package jetbrains.buildServer.clouds.amazon.connector.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public final class AwsExceptionUtils {
  @Nullable
  public static String getAwsErrorMessage(@NotNull final Throwable exception){
    Throwable cause = exception.getCause();

    if (isAmazonServiceException(exception)) {
      AwsServiceException awsServiceException = (AwsServiceException) exception;
      AwsErrorDetails details = awsServiceException.awsErrorDetails();

      if (details == null) {
        return exception.toString();
      }

      return awsServiceException.getMessage();
    } else if (isAmazonServiceException(cause)) {
      AwsServiceException awsServiceException = (AwsServiceException) cause;
      AwsErrorDetails details = awsServiceException.awsErrorDetails();

      if (details == null) {
        return cause.toString();
      }

      return awsServiceException.getMessage();
    } else {
      return exception.getMessage();
    }
  }

  public static boolean isAmazonServiceException(@Nullable final Throwable e){
    return e instanceof AwsServiceException;
  }
}