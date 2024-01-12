

package jetbrains.buildServer.util.amazon.retry.impl;

import com.intellij.openapi.diagnostic.Logger;
import java.util.concurrent.Callable;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import jetbrains.buildServer.util.amazon.retry.ExecuteForAborted;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitrii Bogdanov
 */
public class LoggingRetrierListener extends AbstractRetrierEventListener implements ExecuteForAborted {
  private final Logger myLogger;

  public LoggingRetrierListener(@NotNull final Logger logger) {
    myLogger = logger;
  }

  @Override
  public <T> void beforeRetry(@NotNull final Callable<T> callable, final int retry) {
    myLogger.debug(() -> "Calling [" + callable + "], retry: " + retry + ".");
  }

  @Override
  public <T> void onSuccess(@NotNull final Callable<T> callable, final int retry) {
    myLogger.debug(() -> "Calling [" + callable + "], retry: " + retry + " successful.");
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable, final int retry, @NotNull final Exception e) {
    myLogger.infoAndDebugDetails("Calling [" + callable + "], retry: " + retry + " failed with exception.", e);
  }
}