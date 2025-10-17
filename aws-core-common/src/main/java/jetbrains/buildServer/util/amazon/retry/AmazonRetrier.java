

package jetbrains.buildServer.util.amazon.retry;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import javax.net.ssl.SSLException;
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
    return Retrier.withRetries(retriesNum, Retrier.DelayStrategy.linearBackOff(retryDelay))
                  .registerListener(new LoggingRetrierListener(logger))
                  .registerListener(
                    new AbortingListener(SSLException.class, UnknownHostException.class, SocketException.class, IOException.class, InterruptedException.class) {
                      @Override
                      public <T> void onFailure(@NotNull Callable<T> callable, int retry, @NotNull Exception e) {
                        if (e instanceof InterruptedException) {
                          Thread.currentThread().interrupt();
                          return;
                        }

                        if (e instanceof RecoverableException && ((RecoverableException) e).isRecoverable()) {
                          return;
                        }

                        if (e instanceof SdkClientException && ((SdkClientException) e).retryable()) {
                          return;
                        }

                        super.onFailure(callable, retry, e);
                      }
                    });
  }
}