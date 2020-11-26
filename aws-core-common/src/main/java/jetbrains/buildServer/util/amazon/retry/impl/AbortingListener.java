package jetbrains.buildServer.util.amazon.retry.impl;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import java.util.concurrent.Callable;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import org.jetbrains.annotations.NotNull;

public class AbortingListener extends AbstractRetrierEventListener {
  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable,
                            final int retry,
                            @NotNull final Exception e) {
    if (e instanceof AmazonS3Exception) {
      throw new RuntimeException(e);
    }
  }
}
