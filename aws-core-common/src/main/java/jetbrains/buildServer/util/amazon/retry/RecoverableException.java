package jetbrains.buildServer.util.amazon.retry;

import org.jetbrains.annotations.NotNull;

public abstract class RecoverableException extends RuntimeException {
  public RecoverableException(@NotNull String message) {
    super(message);
  }

  public RecoverableException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }

  public abstract boolean isRecoverable();
}
