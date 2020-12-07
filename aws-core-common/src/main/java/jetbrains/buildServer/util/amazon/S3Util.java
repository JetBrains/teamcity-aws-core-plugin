/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.util.amazon;

import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.AbortableTransfer;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.intellij.openapi.diagnostic.Logger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.amazon.retry.Retrier;
import jetbrains.buildServer.util.amazon.retry.impl.ExponentialDelayListener;
import jetbrains.buildServer.util.amazon.retry.impl.LoggingRetrierListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public final class S3Util {
  private static final int DEFAULT_S3_THREAD_POOL_SIZE = 10;
  @NotNull
  private static final String S3_THREAD_POOL_SIZE = "amazon.s3.transferManager.threadPool.size";
  @NotNull
  private static final Logger LOG = Logger.getInstance(S3Util.class.getName());

  @Used("codedeploy,codebuild,codepipeline")
  @NotNull
  public static <T extends Transfer> Collection<T> withTransferManager(@NotNull AmazonS3 s3Client, @NotNull final WithTransferManager<T> withTransferManager) throws Throwable {
    return withTransferManager(s3Client, withTransferManager, S3AdvancedConfiguration.NULL_CONFIG);
  }

  @NotNull
  public static <T extends Transfer> Collection<T> withTransferManager(@NotNull final AmazonS3 s3Client,
                                                                       @NotNull final WithTransferManager<T> runnable,
                                                                       @NotNull final S3AdvancedConfiguration advancedConfiguration) throws Throwable {

    final TransferManager manager = TransferManagerBuilder.standard()
                                                          .withS3Client(s3Client)
                                                          .withShutDownThreadPools(true)
                                                          .withMinimumUploadPartSize(advancedConfiguration.getMinimumUploadPartSize())
                                                          .withMultipartUploadThreshold(advancedConfiguration.getMultipartUploadThreshold())
                                                          .withMultipartCopyThreshold(advancedConfiguration.getMultipartUploadThreshold())
                                                          .withExecutorFactory(createExecutorFactory(createDefaultExecutorService(advancedConfiguration.getNThreads())))
                                                          .build();

    final Retrier retrier = Retrier.withRetries(advancedConfiguration.getRetriesNum())
                                   .registerListener(new LoggingRetrierListener(LOG))
                                   .registerListener(new ExponentialDelayListener(advancedConfiguration.getRetryDelay()));

    try {
      final List<T> transfers = new ArrayList<>(runnable.run(manager));

      final AtomicBoolean isInterrupted = new AtomicBoolean(false);

      if (runnable instanceof InterruptAwareWithTransferManager) {
        final TransferManagerInterruptHook hook = () -> {
          isInterrupted.set(true);

          for (T transfer : transfers) {
            boolean aborted = false;
            if (transfer instanceof AbortableTransfer) {
              ((AbortableTransfer)transfer).abort();
              aborted = true;
            } else {
              try {
                final Method abort = transfer.getClass().getDeclaredMethod("abort");
                if (abort != null) {
                  abort.invoke(transfer);
                  aborted = true;
                }
              } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
              }
            }
            if (!aborted) {
              LOG.warn("Transfer type " + transfer.getClass().getName() + " does not support interrupt");
            }
          }
        };
        ((InterruptAwareWithTransferManager<T>)runnable).setInterruptHook(hook);
      }

      Throwable exception = null;
      for (T transfer : transfers) {
        try {
          retrier.execute(transfer::waitForCompletion);
        } catch (Throwable t) {
          if (!isInterrupted.get()) {
            if (exception != null) {
              exception.addSuppressed(t);
            } else {
              exception = t;
            }
          }
        }
      }
      if (exception != null) {
        throw exception;
      }

      return CollectionsUtil.filterCollection(transfers, data -> Transfer.TransferState.Completed == data.getState());
    } finally {
      manager.shutdownNow(advancedConfiguration.getShutdownClient());
      if (advancedConfiguration.getShutdownClient()) {
        shutdownClient(s3Client);
      }
    }
  }

  public static void shutdownClient(@NotNull final AmazonS3 s3Client) {
    try {
      LOG.debug(() -> "Shutting down s3 client " + s3Client + " started.");
      s3Client.shutdown();
      LOG.debug(() -> "Shutting down s3 client " + s3Client + " finished.");
    } catch (Exception e) {
      LOG.warnAndDebugDetails("Shutting down s3 client " + s3Client + " failed.", e);
    }
  }

  @NotNull
  private static ExecutorFactory createExecutorFactory(@NotNull final ExecutorService executorService) {
    return () -> executorService;
  }

  public static ExecutorService createDefaultExecutorService(final int nThreads) {
    final ThreadFactory threadFactory = new ThreadFactory() {
      private final AtomicInteger threadCount = new AtomicInteger(1);

      public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("amazon-util-s3-transfer-manager-worker-" + threadCount.getAndIncrement());
        thread.setContextClassLoader(getClass().getClassLoader());
        return thread;
      }
    };
    return Executors.newFixedThreadPool(nThreads, threadFactory);
  }

  public interface WithTransferManager<T extends Transfer> {
    @NotNull
    Collection<T> run(@NotNull TransferManager manager) throws Throwable;
  }

  public interface TransferManagerInterruptHook {
    void interrupt() throws Throwable;
  }

  public interface InterruptAwareWithTransferManager<T extends Transfer> extends WithTransferManager<T> {
    /**
     * This method is executed after {@link WithTransferManager#run(TransferManager)} is completed,
     * aiming to stop current execution
     *
     * @param hook a callback to interrupt
     */
    void setInterruptHook(@NotNull TransferManagerInterruptHook hook);
  }

  public static class S3AdvancedConfiguration {
    @NotNull
    private static final S3AdvancedConfiguration NULL_CONFIG = new S3AdvancedConfiguration().withRetryDelayMs(0).withRetryNum(0);
    @Nullable
    private Long myMinimumUploadPartSize;
    @Nullable
    private Long myMultipartUploadThreshold;
    @Nullable
    private Integer connectionTimeout = null;
    private boolean shutdownClient = false;
    private int nRetries = 0;
    private int retryDelay = 0;
    private int nThreads = TeamCityProperties.getInteger(S3_THREAD_POOL_SIZE, DEFAULT_S3_THREAD_POOL_SIZE);

    @Nullable
    public Long getMinimumUploadPartSize() {
      return myMinimumUploadPartSize;
    }

    public S3AdvancedConfiguration withMinimumUploadPartSize(@Nullable final Long multipartChunkSize) {
      myMinimumUploadPartSize = multipartChunkSize;
      return this;
    }

    @Nullable
    public Long getMultipartUploadThreshold() {
      return myMultipartUploadThreshold;
    }

    @Nullable
    public Integer getConnectionTimeout() {
      return connectionTimeout;
    }

    @NotNull
    public S3AdvancedConfiguration withConnectionTimeout(@Nullable final Integer connectionTimeout) {
      if (connectionTimeout == null || connectionTimeout == -1) {
        this.connectionTimeout = null;
      } else {
        this.connectionTimeout = connectionTimeout;
      }
      return this;
    }

    public S3AdvancedConfiguration withMultipartUploadThreshold(@Nullable final Long multipartThreshold) {
      myMultipartUploadThreshold = multipartThreshold;
      return this;
    }

    public boolean getShutdownClient() {
      return shutdownClient;
    }

    @NotNull
    public S3AdvancedConfiguration withShutdownClient() {
      this.shutdownClient = true;
      return this;
    }

    @NotNull
    public S3AdvancedConfiguration withRetryNum(final int nRetries) {
      if (nRetries < 0) {
        throw new IllegalArgumentException("nRetries should be >= 0");
      }
      this.nRetries = nRetries;
      return this;
    }

    @NotNull
    public S3AdvancedConfiguration withRetryDelayMs(final int delayMs) {
      if (delayMs < 0) {
        throw new IllegalArgumentException("delayMs should be >= 0");
      }
      this.retryDelay = delayMs;
      return this;
    }

    @SuppressWarnings("unused")
    @NotNull
    public S3AdvancedConfiguration withNThreads(final int nThreads) {
      if (nThreads < 1) {
        throw new IllegalArgumentException("NThreads should be > 0");
      }
      this.nThreads = nThreads;
      return this;
    }

    public int getRetriesNum() {
      return nRetries;
    }

    public int getRetryDelay() {
      return retryDelay;
    }

    public int getNThreads() {
      return nThreads;
    }
  }
}
