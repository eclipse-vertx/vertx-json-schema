package io.vertx.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.TestUtils;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;

import static io.vertx.json.schema.asserts.MyAssertions.assertThat;

@ExtendWith(VertxExtension.class)
public class SchemaImplTest {

  @Test
  public void testFailureInputScope(Vertx vertx) throws IOException {
    URI u = TestUtils.buildBaseUri("schema_impl_test", "arrays.json");
    JsonObject obj = TestUtils.loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema)
      .isSync();
    assertThat(schema)
      .validateAsyncFailure(10)
      .extracting(ValidationException::inputScope)
      .isEqualTo(JsonPointer.create());
    assertThat(schema)
      .validateAsyncFailure(new JsonObject().put("a", 0))
      .extracting(ValidationException::inputScope)
      .isEqualTo(JsonPointer.from("/a"));
    assertThat(schema)
      .validateAsyncFailure(new JsonObject().put("a", new JsonArray()
        .add(new JsonObject().put("inner", 10))
        .add(new JsonObject().put("inner", "fail"))
      ))
      .extracting(ValidationException::inputScope)
      .isEqualTo(JsonPointer.from("/a/1/inner"));
  }

}
