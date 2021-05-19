/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import java.util.concurrent.Callable;
import jetbrains.buildServer.util.amazon.retry.impl.NoRetryRetrierImpl;
import jetbrains.buildServer.util.amazon.retry.impl.RetrierImpl;
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

  <T> T execute(@NotNull final Callable<T> callable);

  default void execute(@NotNull final ExceptionalRunnable runnable) {
    execute(() -> {
      runnable.run();
      return null;
    });
  }

  @NotNull
  Retrier registerListener(@NotNull RetrierEventListener retrier);

  interface ExceptionalRunnable {
    void run() throws Exception;
  }
}
