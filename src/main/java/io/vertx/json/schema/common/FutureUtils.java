/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.common;

import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FutureUtils {

  public static <T> Future<T> oneOf(List<Future<T>> results) {
    final Promise<T> res = Promise.promise();
    final AtomicInteger processed = new AtomicInteger(0);
    final AtomicBoolean atLeastOneOk = new AtomicBoolean(false);
    final AtomicReference<T> result = new AtomicReference<>();
    final int len = results.size();
    for (Future<T> tFuture : results) {
      tFuture.onComplete(ar -> {
        int p = processed.incrementAndGet();
        if (ar.succeeded()) {
          if (atLeastOneOk.get())
            res.tryFail(new IllegalStateException("One future was already completed"));
          else {
            atLeastOneOk.set(true);
            result.set(res.future().result());
          }
        }
        if (p == len) {
          if (atLeastOneOk.get()) res.tryComplete(result.get());
          else res.tryFail(new IllegalStateException(ar.cause()));
        }
      });
    }
    return res.future();
  }

}
