package io.vertx.ext.json.schema.asserts;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaRouter;

public class MyAssertions {

  public static SchemaAssert assertThat(Schema actual) {
    return new SchemaAssert(actual);
  }

  public static SchemaRouterAssert assertThat(SchemaRouter actual) {
    return new SchemaRouterAssert(actual);
  }

  public static JsonAssert assertThat(JsonObject actual) { return new JsonAssert(actual); }

  public static JsonAssert assertThat(JsonArray actual) { return new JsonAssert(actual); }

  public static JsonAssert assertThatJson(Object actual) { return new JsonAssert(actual); }

}
