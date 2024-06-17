

package jetbrains.buildServer.util.amazon.retry.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.amazon.retry.AmazonRetrier;
import jetbrains.buildServer.util.retry.AbortRetriesException;
import jetbrains.buildServer.util.retry.RecoverableException;
import jetbrains.buildServer.util.retry.Retrier;
import jetbrains.buildServer.util.retry.RetrierEventListener;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Dmitrii Bogdanov
 */
@Test
public class RetrierTest extends BaseTestCase {
  private final CounterListener myCounterListener = new CounterListener();

  @BeforeMethod
  public void setUp() {
    myCounterListener.reset();
  }

  @Test
  void testDefaultRetrierKeepsInterruptedStatus() {
    BaseTestCase.assertExceptionThrown(() -> {
      AmazonRetrier.defaultAwsRetrier(5, 1000, Loggers.TEST).execute(() -> {
        Thread.currentThread().interrupt();
        throw new InterruptedException();
      });
    }, AbortRetriesException.class);
    Assert.assertTrue(Thread.currentThread().isInterrupted());
  }

  @Test
  void testDefaultRetrierRetriesExpectedExceptions() {
    final Retrier retrier = AmazonRetrier.defaultAwsRetrier(5, 0, Loggers.TEST).registerListener(myCounterListener);
    final AtomicInteger counter = new AtomicInteger();
    retrier.execute(() -> {
      if (counter.getAndIncrement() < 4) {
        throw new RecoverableException("Got error") {
          @Override
          public boolean isRecoverable() {
            return true;
          }
        };
      }
    });
    Assert.assertEquals(myCounterListener.getNumberOfRetries(), 4);
    Assert.assertEquals(myCounterListener.getNumberOfFailures(), 4);
  }

  @Test
  void testDefaultRetrierRetriesExpectedExceptionsFromMapper() {
    final Retrier retrier = AmazonRetrier.defaultAwsRetrier(5, 0, Loggers.TEST).registerListener(myCounterListener);
    final AtomicInteger counter = new AtomicInteger();

    List<Object> someObjects = Collections.singletonList(new Object());
    someObjects
      .stream()
      .map(
        retrier.retryableMapper(
          it -> {
            if (counter.getAndIncrement() < 4) {
              throw new RecoverableException("Got error") {
                @Override
                public boolean isRecoverable() {
                  return true;
                }
              };
            } else {
              return null;
            }
          })
      ).collect(Collectors.toList());
    Assert.assertEquals(myCounterListener.getNumberOfRetries(), 4);
    Assert.assertEquals(myCounterListener.getNumberOfFailures(), 4);
  }


  /**
   * @author Dmitrii Bogdanov
   */
  public static class CounterListener implements RetrierEventListener {
    private final AtomicInteger myNumberOfRetries = new AtomicInteger();
    private final AtomicInteger myNumberOfFailures = new AtomicInteger(0);
    private final AtomicLong myExecutionTimeMs = new AtomicLong();

    @Override
    public <T> void beforeExecution(@NotNull final Callable<T> callable) {
      myExecutionTimeMs.set(System.currentTimeMillis());
    }

    @Override
    public <T> void beforeRetry(@NotNull final Callable<T> callable, final int retry) {
      myNumberOfRetries.set(retry);
    }

    @Override
    public <T> void afterExecution(@NotNull final Callable<T> callable) {
      myExecutionTimeMs.addAndGet(-System.currentTimeMillis());
    }

    @Override
    public <T> void onFailure(@NotNull final Callable<T> callable, final int retry, @NotNull final Exception e) {
      myNumberOfFailures.incrementAndGet();
    }

    int getNumberOfRetries() {
      return myNumberOfRetries.get();
    }

    int getNumberOfFailures() {
      return myNumberOfFailures.get();
    }

    long getExecutionTimeInMs() {
      return -myExecutionTimeMs.get();
    }

    public void reset() {
      myNumberOfRetries.set(0);
      myNumberOfFailures.set(0);
      myExecutionTimeMs.set(0);
    }
  }
}