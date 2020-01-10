package io.vertx.ext.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.draft7.Draft7SchemaParser;
import io.vertx.ext.json.schema.common.SchemaParserInternal;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(VertxExtension.class)
public class DefaultValuesApplyTest {

  @Test
  public void simpleDefault(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "simple_default.json");
    JsonObject obj = loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema)
        .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", "francesco")))
        .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject();
    assertThatCode(() -> schema.applyDefaultValues(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
        .extracting(JsonPointer.create().append("a"))
        .isEqualTo("hello");
  }

  @Test
  public void nested(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "nested.json");
    JsonObject obj = loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema)
        .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", new JsonObject())))
        .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject().put("a", new JsonObject());
    assertThatCode(() -> schema.applyDefaultValues(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
        .extracting(JsonPointer.create().append("b"))
        .isEqualTo("b_default");
    assertThatJson(objToApplyDefaults)
        .extracting(JsonPointer.create().append("a").append("c"))
        .isEqualTo(0);
  }

  @Test
  public void arrays(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "arrays.json");
    JsonObject obj = loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema)
        .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", new JsonArray().add(new JsonObject().put("inner", 1)))))
        .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject().put("a", new JsonArray().add(new JsonObject()).add(new JsonObject()));
    assertThatCode(() -> schema.applyDefaultValues(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
        .extracting(JsonPointer.create().append("a").append("0").append("inner"))
        .isEqualTo(0);
    assertThatJson(objToApplyDefaults)
        .extracting(JsonPointer.create().append("a").append("1").append("inner"))
        .isEqualTo(0);
  }

  @Test
  public void ref(Vertx vertx, VertxTestContext testContext) throws IOException {
    URI u = buildBaseUri("default_test", "ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    router
        .solveAllSchemaReferences(schema)
        .setHandler(testContext.succeeding(s -> {
          testContext.verify(() -> {
            assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
                .doesNotThrowAnyException();

            JsonObject objToApplyDefaults = new JsonObject();
            assertThatCode(() -> schema.applyDefaultValues(objToApplyDefaults)).doesNotThrowAnyException();
            assertThatJson(objToApplyDefaults)
                .extracting(JsonPointer.create().append("hello"))
                .isEqualTo("world");
          });
          testContext.completeNow();
        }));
  }

  @Test
  public void circularRef(Vertx vertx, VertxTestContext testContext) throws IOException {
    URI u = buildBaseUri("default_test", "circular_ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    router
        .solveAllSchemaReferences(schema)
        .setHandler(testContext.succeeding(s -> {
          testContext.verify(() -> {
            assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", new JsonObject())))
                .doesNotThrowAnyException();

            JsonObject objToApplyDefaults = new JsonObject().put("hello", new JsonObject());
            assertThatCode(() -> schema.applyDefaultValues(objToApplyDefaults)).doesNotThrowAnyException();
            assertThatJson(objToApplyDefaults)
                .extracting(JsonPointer.create().append("hello").append("name"))
                .isEqualTo("world");
            assertThatJson(objToApplyDefaults)
                .extracting(JsonPointer.create().append("hello").append("and"))
                .isEqualTo(new JsonObject().put("hello", new JsonObject().put("name", "francesco")));
          });
          testContext.completeNow();
        }));
  }

}
