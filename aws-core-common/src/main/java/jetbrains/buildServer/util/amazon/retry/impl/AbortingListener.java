/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.amazon.retry.AbortRetriesException;
import jetbrains.buildServer.util.amazon.retry.AbstractRetrierEventListener;
import jetbrains.buildServer.util.amazon.retry.ExecuteForAborted;
import org.jetbrains.annotations.NotNull;

public class AbortingListener extends AbstractRetrierEventListener implements ExecuteForAborted {
  @NotNull
  private final List<Class<? extends Exception>> retryableExceptions = new ArrayList<>();

  @SafeVarargs
  public AbortingListener(@NotNull final Class<? extends Exception>... retryableExceptions) {
    this.retryableExceptions.addAll(Arrays.asList(retryableExceptions));
  }

  @Override
  public <T> void onFailure(@NotNull final Callable<T> callable,
                            final int retry,
                            @NotNull final Exception e) {
    if (retryableExceptions.stream().noneMatch(retryableException -> ExceptionUtil.getCause(e, retryableException) != null)) {
      throw new AbortRetriesException(e);
    }
  }
}
