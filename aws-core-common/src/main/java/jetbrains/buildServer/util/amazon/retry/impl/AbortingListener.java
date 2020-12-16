package jetbrains.buildServer.util.amazon.retry.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import org.jetbrains.annotations.NotNull;

public class AbortingListener extends AbstractRetrierEventListener {
  @NotNull
  private final List<Class<? extends Exception>> retryableExceptions = new ArrayList<>();

  @SafeVarargs
  public AbortingListener(@NotNull final Class<? extends Exception>... retryableExceptions) {
    this.retryableExceptions.addAll(Arrays.asList(retryableExceptions));
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable,
                            final int retry,
                            @NotNull final Exception e) {
    if (retryableExceptions.stream().noneMatch(aClass -> aClass.isAssignableFrom(e.getClass()))) {
      ExceptionUtil.rethrowAsRuntimeException(e);
    }
  }
}
