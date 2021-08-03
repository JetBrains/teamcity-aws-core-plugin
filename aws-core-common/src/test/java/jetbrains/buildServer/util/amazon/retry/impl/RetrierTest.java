/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.util.amazon.retry.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.amazon.retry.AbortRetriesException;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import jetbrains.buildServer.util.amazon.retry.ExecuteForAborted;
import jetbrains.buildServer.util.amazon.retry.Retrier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
  void testNumberOfRetriesIsCorrect() {
    final CounterListener counter = new CounterListener();
    try {
      new RetrierImpl(5)
        .registerListener(counter)
        .execute((Callable<Integer>)() -> {
          throw new DummyRuntimeException("Oops!");
        });
    } catch (Exception ignored) {
    }
    Assert.assertEquals(counter.getNumberOfFailures(), 6);
    Assert.assertEquals(counter.getNumberOfRetries(), 5);
  }

  @Test
  void testNumberOfRetriesIsWhenAbortingListenerIsRegisteredForExceptionClass() {
    final CounterListener counter = new CounterListener();
    try {
      new RetrierImpl(5)
        .registerListener(new AbortingListener(RuntimeException.class))
        .registerListener(counter)
        .execute((Callable<Integer>)() -> {
          throw new DummyRuntimeException("Oops!");
        });
    } catch (Exception ignored) {
    }
    Assert.assertEquals(counter.getNumberOfFailures(), 6);
    Assert.assertEquals(counter.getNumberOfRetries(), 5);
  }

  @Test
  void testInterruptedExceptionKeepsInterruptedStatus() {
    final CounterListener counter = new CounterListener();
    BaseTestCase.assertExceptionThrown(() -> {
      new RetrierImpl(5)
        .registerListener(new AbortingListener())
        .registerListener(counter)
        .execute(() -> {
          Thread.currentThread().interrupt();
          throw new InterruptedException();
        });
    }, AbortRetriesException.class);
    Assert.assertTrue(Thread.currentThread().isInterrupted());
    Assert.assertEquals(counter.getNumberOfFailures(), 1);
    Assert.assertEquals(counter.getNumberOfRetries(), 0);
  }

  @Test
  void testDefaultRetrierKeepsInterruptedStatus() {
    BaseTestCase.assertExceptionThrown(() -> {
      Retrier.defaultRetrier(5, 1000, Loggers.TEST).execute(() -> {
        Thread.currentThread().interrupt();
        throw new InterruptedException();
      });
    }, AbortRetriesException.class);
    Assert.assertTrue(Thread.currentThread().isInterrupted());
  }

  @Test
  void testNumberOfRetriesIsWhenAbortingListenerIsRegisteredForDifferentClass() {
    final CounterListener counter = new CounterListener();
    try {
      new RetrierImpl(5)
        .registerListener(new AbortingListener(ClassNotFoundException.class))
        .registerListener(counter)
        .execute((Callable<Integer>)() -> {
          throw new DummyRuntimeException("Oops!");
        });
    } catch (Exception ignored) {
    }
    Assert.assertEquals(counter.getNumberOfFailures(), 1);
    Assert.assertEquals(counter.getNumberOfRetries(), 0);
  }

  @Test
  void testRuntimeExceptionThrownUnchanged() {
    final DummyRuntimeException expected = new DummyRuntimeException("Oops!");
    try {
      new RetrierImpl(1).execute((Callable<Integer>)() -> {
        throw expected;
      });
    } catch (Exception actual) {
      Assert.assertEquals(actual, expected);
    }
  }

  @Test
  void testCheckedExceptionThrownAsRuntimeWithSameMessage() {
    try {
      new RetrierImpl(2).execute((Callable<Integer>)() -> {
        throw new DummyCheckedException("Oops!");
      });
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), RuntimeException.class);
      Assert.assertEquals(e.getMessage(), "Oops!");
      Assert.assertEquals(e.getCause().getClass(), DummyCheckedException.class);
      Assert.assertEquals(e.getCause().getMessage(), "Oops!");
    }
    try {
      new RetrierImpl(2).execute((Callable<Integer>)() -> {
        throw new DummyCheckedException(null);
      });
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), RuntimeException.class);
      Assert.assertNull(e.getMessage());
      Assert.assertEquals(e.getCause().getClass(), DummyCheckedException.class);
      Assert.assertNull(e.getCause().getMessage());
    }
  }

  @Test
  void testRetrierReturnsCorrectResultWhenFailedLessTimesThanNumberOfRetries() {
    final String result = new RetrierImpl(10)
      .registerListener(myCounterListener)
      .execute(failingNTimes(1, "expected"));
    Assert.assertEquals(result, "expected");
    Assert.assertEquals(myCounterListener.getNumberOfFailures(), 1);
    Assert.assertEquals(myCounterListener.getNumberOfRetries(), 1);
  }

  @SuppressWarnings("SameParameterValue")
  @NotNull
  private <T> Callable<T> failingNTimes(final int nTimesToFail, final T retVal) {
    return () -> {
      if (myCounterListener.getNumberOfFailures() < nTimesToFail) {
        throw new DummyRuntimeException("Oops!");
      } else {
        return retVal;
      }
    };
  }

  private static final class DummyRuntimeException extends RuntimeException {
    public DummyRuntimeException(final String message) {
      super(message);
    }
  }

  private static final class DummyCheckedException extends Exception {
    public DummyCheckedException(@Nullable final String message) {
      super(message);
    }
  }


  /**
   * @author Dmitrii Bogdanov
   */
  public static class CounterListener extends AbstractRetrierEventListener implements ExecuteForAborted {
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
