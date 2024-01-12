

package jetbrains.buildServer.util.amazon.retry.impl;

import com.intellij.openapi.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.amazon.retry.AbortRetriesException;
import jetbrains.buildServer.util.amazon.retry.ExecuteForAborted;
import jetbrains.buildServer.util.amazon.retry.Retrier;
import jetbrains.buildServer.util.amazon.retry.RetrierEventListener;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitrii Bogdanov
 */
public class RetrierImpl implements Retrier {
  private final int myMaxRetries;
  private final List<RetrierEventListener> myRetrierEventListeners = new ArrayList<>();

  public RetrierImpl(final int maxRetries) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("Number of retries should be greater than 0");
    }
    myMaxRetries = maxRetries;
  }

  @Override
  public <T> T execute(@NotNull final Callable<T> callable) {
    beforeExecution(callable);
    RuntimeException exception = null;
    for (int retry = 0; retry <= myMaxRetries; retry++) {
      try {
        beforeRetry(callable, retry);
        final T call = callable.call();
        onSuccess(callable, retry);
        afterExecution(callable);
        return call;
      } catch (Exception e) {
        exception = asRuntimeException(e);
        try {
          onFailure(callable, retry, exception);
        } catch (AbortRetriesException abortRetriesException) {
          ExceptionUtil.rethrowAsRuntimeException(abortRetriesException.getCause());
        }
      }
    }
    assert exception != null : "If we got here, exception cannot be null";
    throw exception;
  }

  private <T> void retryAsync(int remainingRetries,
                              Throwable lastException,
                              CompletableFuture<Pair<T, Integer>> futureResult,
                              Supplier<CompletableFuture<T>> operationSupplier) {
    // passing fake callable mainly for logging purposes
    beforeRetry(() -> null, myMaxRetries - remainingRetries);

    if (remainingRetries <= 0) {
      futureResult.completeExceptionally(lastException);
      return;
    }

    operationSupplier.get().whenComplete((result, error) -> {
      if (error == null) {
        futureResult.complete(new Pair<>(result, myMaxRetries - remainingRetries));
      } else {
        RuntimeException e = asRuntimeException(error);

        try {
          // passing fake callable, while we don't have real callable to pass to related function.
          // this::onFailure will either complets successfully or throw "final" exception that will be handled by
          // the result.handle method in this::executeAsync
          onFailure(() -> null, myMaxRetries - remainingRetries, e);
          retryAsync(remainingRetries - 1, e, futureResult, operationSupplier);
        } catch (AbortRetriesException abortRetriesException) {
          futureResult.completeExceptionally(abortRetriesException.getCause());
        } catch (Throwable t) {
          futureResult.completeExceptionally(t);
        }
      }
    });
  }

  @NotNull
  private static RuntimeException asRuntimeException(Throwable error) {
    // wrap exception into RuntimeException if needed
    RuntimeException e;
    if (error instanceof RuntimeException) {
      e = (RuntimeException)error;
    } else if (error instanceof InterruptedException) {
      e = new AbortRetriesException(error);
    } else {
      e = new RuntimeException(error.getMessage(), error);
    }
    return e;
  }

  @Override
  public <T> CompletableFuture<T> executeAsync(@NotNull final Supplier<CompletableFuture<T>> futureSupplier) {
    final CompletableFuture<Pair<T, Integer>> result = new CompletableFuture<>();
    // we don't have real callable to pass to related function.
    // gladly, they don't evaluate it with a :call method.
    beforeExecution(() -> null);

    // retry Async may throw exception in case of error in futureSupplier or ::beforeRetry call.
    try {
      retryAsync(myMaxRetries, null, result, futureSupplier);
    } catch (Exception e) {
      CompletableFuture<T> failure = new CompletableFuture<>();
      failure.completeExceptionally(e);
      return failure;
    }

    return result.handle((pair, error) -> {
      if (error != null) {
        // exception occured
        ExceptionUtil.rethrowAsRuntimeException(error);
      } else {
        final T resultValue = pair.getFirst();
        // result is ready
        onSuccess(() -> resultValue, pair.getSecond());
        afterExecution(() -> resultValue);
        return resultValue;
      }
      return null;
    });
  }

  @NotNull
  @Override
  public Retrier registerListener(@NotNull final RetrierEventListener retrierEventListener) {
    myRetrierEventListeners.add(retrierEventListener);
    return this;
  }

  @Override
  public <T> void beforeExecution(@NotNull final Callable<T> callable) {
    for (final RetrierEventListener retrierEventListener : myRetrierEventListeners) {
      retrierEventListener.beforeExecution(callable);
    }
  }

  @Override
  public <T> void beforeRetry(@NotNull final Callable<T> callable, final int retry) {
    for (final RetrierEventListener retrierEventListener : myRetrierEventListeners) {
      retrierEventListener.beforeRetry(callable, retry);
    }
  }

  @Override
  public <T> void onSuccess(@NotNull final Callable<T> callable, final int retry) {
    for (final RetrierEventListener retrierEventListener : myRetrierEventListeners) {
      retrierEventListener.onSuccess(callable, retry);
    }
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable, final int retry, @NotNull final Exception e) {
    final AtomicBoolean retriesAborted = new AtomicBoolean(e instanceof AbortRetriesException);
    final AtomicReference<Exception> thrownExceptions = new AtomicReference<>(null);
    for (final RetrierEventListener retrierEventListener : myRetrierEventListeners) {
      try {
        if (!retriesAborted.get() || retrierEventListener instanceof ExecuteForAborted) {
          retrierEventListener.onFailure(callable, retry, e);
        }
      } catch (Exception exception) {
        if (!thrownExceptions.compareAndSet(null, exception) && !thrownExceptions.compareAndSet(exception, exception)) {
          thrownExceptions.get().addSuppressed(exception);
          if (exception instanceof AbortRetriesException) {
            retriesAborted.set(true);
          }
        }
      }
    }
    final Exception thrownException = thrownExceptions.get();
    if (thrownException != null) {
      ExceptionUtil.rethrowAsRuntimeException(thrownException);
    }
  }

  @Override
  public <T> void afterExecution(@NotNull final Callable<T> callable) {
    for (final RetrierEventListener retrierEventListener : myRetrierEventListeners) {
      retrierEventListener.afterExecution(callable);
    }
  }
}