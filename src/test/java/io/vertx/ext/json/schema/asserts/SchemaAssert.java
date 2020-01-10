package io.vertx.ext.json.schema.asserts;

import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.common.SchemaImpl;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.StringAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaAssert extends AbstractAssert<SchemaAssert, Schema> {

  public SchemaAssert(Schema actual) {
    super(actual, SchemaAssert.class);
  }

  public StringAssert hasXId() {
    isNotNull();

    if (!(actual instanceof SchemaImpl))
      failWithMessage("Schema <%s> must be a SchemaImpl instance", actual.toString());

    return new StringAssert(((SchemaImpl)actual).getJson().getString("x-id"));
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

}
