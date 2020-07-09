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
