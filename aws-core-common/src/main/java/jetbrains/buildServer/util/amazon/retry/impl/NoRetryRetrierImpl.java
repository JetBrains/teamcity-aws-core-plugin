

package jetbrains.buildServer.util.amazon.retry.impl;

import java.util.concurrent.Callable;
import org.jetbrains.annotations.NotNull;

public class NoRetryRetrierImpl extends RetrierImpl {
  public NoRetryRetrierImpl() {
    super(0);
  }

  @Override
  public <T> T execute(@NotNull Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e.getMessage(), e);
    }
  }
}