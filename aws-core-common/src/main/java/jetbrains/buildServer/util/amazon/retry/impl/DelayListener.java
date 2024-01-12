

package jetbrains.buildServer.util.amazon.retry.impl;

import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.amazon.retry.AbortRetriesException;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public class DelayListener extends AbstractRetrierEventListener {
  private final long myDelay;

  public DelayListener(final long delayMs) {
    myDelay = delayMs;
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable, final int retry, @NotNull final Exception e) {
    if (!Thread.currentThread().isInterrupted() && !(e instanceof AbortRetriesException) && myDelay > 0) {
      ThreadUtil.sleep(myDelay);
    }
  }
}