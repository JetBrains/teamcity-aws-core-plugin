package jetbrains.buildServer.util.amazon.retry;

import org.jetbrains.annotations.NotNull;

public class AbortRetriesException extends RuntimeException {
  public AbortRetriesException(@NotNull final Throwable cause) {
    super(cause);
  }
}
