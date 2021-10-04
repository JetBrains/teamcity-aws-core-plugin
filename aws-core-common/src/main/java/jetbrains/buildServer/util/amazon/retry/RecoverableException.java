package jetbrains.buildServer.util.amazon.retry;

import jetbrains.buildServer.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

public abstract class RecoverableException extends RuntimeException {
  public RecoverableException(@NotNull String message) {
    super(message);
  }

  public RecoverableException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }

  protected static boolean isRecoverable(@NotNull final Throwable cause) {
    final RecoverableException recoverableException = ExceptionUtil.getCause(cause, RecoverableException.class);
    if (recoverableException != null) {
      return recoverableException.isRecoverable();
    }
    return false;
  }

  public abstract boolean isRecoverable();
}
