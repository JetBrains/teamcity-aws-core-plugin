

package jetbrains.buildServer.util.amazon.retry.impl;

import java.util.concurrent.Callable;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.amazon.retry.AbortRetriesException;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitrii Bogdanov
 */
public class ExponentialDelayListener extends AbstractRetrierEventListener {
  private final long myDelay;

  public ExponentialDelayListener(final long delayMs) {
    myDelay = delayMs;
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable, final int retry, @NotNull final Exception e) {
    if (!Thread.currentThread().isInterrupted() && !(e instanceof AbortRetriesException) && myDelay > 0) {
      ThreadUtil.sleep(myDelay * (retry + 1));
    }
  }
}