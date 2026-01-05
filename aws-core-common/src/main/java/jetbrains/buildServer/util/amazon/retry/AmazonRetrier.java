

package jetbrains.buildServer.util.amazon.retry;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.retry.RecoverableException;
import jetbrains.buildServer.util.retry.Retrier;
import jetbrains.buildServer.util.retry.RetrierEventListener;
import jetbrains.buildServer.util.retry.impl.AbortingListener;
import jetbrains.buildServer.util.retry.impl.LoggingRetrierListener;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * @author Dmitrii Bogdanov
 */
public interface AmazonRetrier extends RetrierEventListener {

  @NotNull
  static Retrier defaultAwsRetrier(final int retriesNum, final int retryDelay, @NotNull final Logger logger) {
    return Retrier.withAttempts(retriesNum + 1, Retrier.DelayStrategy.linearBackOff(retryDelay))
                  .registerListener(new LoggingRetrierListener(logger))
                  .registerListener(
                    new AbortingListener(ExecutionException.class, SSLException.class, UnknownHostException.class, SocketException.class, InterruptedIOException.class, InterruptedException.class, IOException.class) {
                      @Override
                      public <T> void onFailure(@NotNull Callable<T> callable, int retry, @NotNull Exception e) {
                        if (e instanceof InterruptedException) {
                          Thread.currentThread().interrupt();
                          return;
                        }

                        final RecoverableException recoverableException = ExceptionUtil.getCause(e, RecoverableException.class);
                        if (recoverableException != null && recoverableException.isRecoverable()) {
                          return;
                        }

                        final SdkClientException sdkClientException = ExceptionUtil.getCause(e, SdkClientException.class);
                        if (sdkClientException != null && sdkClientException.retryable()) {
                          return;
                        }

                        super.onFailure(callable, retry, e);
                      }
                    });
  }
}