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

package jetbrains.buildServer.util.amazon.retry;

import com.amazonaws.SdkClientException;
import com.amazonaws.retry.RetryUtils;
import com.intellij.openapi.diagnostic.Logger;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.function.Function;
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
                    new AbortingListener(SSLException.class, UnknownHostException.class, SocketException.class, InterruptedIOException.class, InterruptedException.class) {
                      @Override
                      public <T> void onFailure(@NotNull Callable<T> callable, int retry, @NotNull Exception e) {
                        if (e instanceof InterruptedException) {
                          Thread.currentThread().interrupt();
                          return;
                        } else if (e instanceof RecoverableException && ((RecoverableException)e).isRecoverable()) {
                          return;
                        } else if (e instanceof SdkClientException && RetryUtils.isRetryableServiceException((SdkClientException)e)) {
                          return;
                        }
                        super.onFailure(callable, retry, e);
                      }
                    })
                  .registerListener(new ExponentialDelayListener(retryDelay));
  }

  <T> T execute(@NotNull final Callable<T> callable);

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
