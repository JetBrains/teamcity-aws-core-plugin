

package jetbrains.buildServer.util.amazon.retry;

import com.amazonaws.SdkClientException;
import com.amazonaws.retry.RetryUtils;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.net.ssl.SSLException;
import jetbrains.buildServer.util.amazon.retry.impl.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitrii Bogdanov
 */
public interface Retrier extends RetrierEventListener {
  static Retrier withRetries(final int nRetries) {
    if (nRetries < 0) {
      throw new IllegalArgumentException("nRetries should be a positive number");
    } else if (nRetries == 0) {
      return new NoRetryRetrierImpl();
    } else {
      return new RetrierImpl(nRetries);
    }
  }

  @NotNull
  static Retrier defaultRetrier(final int retriesNum, final int retryDelay, @NotNull final Logger logger) {
    return Retrier.withRetries(retriesNum)
                  .registerListener(new LoggingRetrierListener(logger))
                  .registerListener(
                    new AbortingListener(SSLException.class, UnknownHostException.class, SocketException.class, IOException.class, InterruptedException.class) {
                      @Override
                      public <T> void onFailure(@NotNull Callable<T> callable, int retry, @NotNull Exception e) {
                        if (e instanceof InterruptedException) {
                          Thread.currentThread().interrupt();
                          return;
                        }
                        if (e instanceof RecoverableException && ((RecoverableException)e).isRecoverable()) {
                          return;
                        }
                        if (e instanceof SdkClientException && RetryUtils.isRetryableServiceException((SdkClientException)e)) {
                          return;
                        }
                        super.onFailure(callable, retry, e);
                      }
                    })
                  .registerListener(new ExponentialDelayListener(retryDelay));
  }

  <T> T execute(@NotNull final Callable<T> callable);

  <T> CompletableFuture<T> executeAsync(@NotNull final Supplier<CompletableFuture<T>> futureSupplier);

  default void execute(@NotNull final ExceptionalRunnable runnable) {
    execute(() -> {
      runnable.run();
      return null;
    });
  }

  @NotNull
  default <T, R> Function<T, R> retryableMapper(@NotNull final Function<T, R> function) {
    return (t) -> execute(() -> function.apply(t));
  }

  @NotNull
  Retrier registerListener(@NotNull RetrierEventListener retrier);

  interface ExceptionalRunnable {
    void run() throws Exception;
  }
}