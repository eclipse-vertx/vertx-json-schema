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
package io.vertx.ext.json.schema.asserts;

import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.common.SchemaImpl;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.StringAssert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SchemaAssert extends AbstractAssert<SchemaAssert, Schema> {

  public SchemaAssert(Schema actual) {
    super(actual, SchemaAssert.class);
  }

  public StringAssert hasXId() {
    isNotNull();

    if (!(actual instanceof SchemaImpl))
      failWithMessage("Schema <%s> must be a SchemaImpl instance", actual.toString());

    return new StringAssert(((SchemaImpl) actual).getJson().getString("x-id"));
  }

  public SchemaAssert hasXIdEqualsTo(String expectedXId) {
    hasXId().isEqualTo(expectedXId);
    return this;
  }

  public SchemaAssert isSync() {
    assertThat(actual.isSync()).isTrue();
    return this;
  }

  public SchemaAssert isAsync() {
    assertThat(actual.isSync()).isFalse();
    return this;
  }

  public SchemaAssert validateAsyncSuccess(Object in) {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ex = new AtomicReference<>();
    actual.validateAsync(in).onComplete(ar -> {
      ex.set(ar.cause());
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      fail("Failure while waiting for schema to validate", e);
    }
    assertThat(ex.get())
      .isNull();
    return this;
  }

  public SchemaAssert validateAsyncFailure(Object in) {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ex = new AtomicReference<>();
    actual.validateAsync(in).onComplete(ar -> {
      ex.set(ar.cause());
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      fail("Failure while waiting for schema to validate", e);
    }
    assertThat(ex.get())
      .withFailMessage("Expecting schema to validate with a failure")
      .isNotNull();
    return this;
  }

}
