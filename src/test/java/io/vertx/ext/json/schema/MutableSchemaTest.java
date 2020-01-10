package io.vertx.ext.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(VertxExtension.class)
public class MutableSchemaTest {

  @Test
  public void schemaMustBeSyncBeforeValidation(Vertx vertx) throws IOException {
    URI u = buildBaseUri("mutable_schema_test", "sync_1.json");
    JsonObject obj = loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema).isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void testRefToDependencies(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = buildBaseUri("mutable_schema_test", "async_ref_1.json");
    JsonObject obj = loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
        .isInstanceOf(NoSyncValidationException.class);

    schema
        .validateAsync(new JsonObject().put("hello", "a"))
        .setHandler(testContext.succeeding(r -> {
      testContext.verify(() -> {
        assertThat(schema)
            .isSync();
        assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
            .isInstanceOf(ValidationException.class);
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testRefToDependenciesPreSolved(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = buildBaseUri("mutable_schema_test", "async_ref_1.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
        .isInstanceOf(NoSyncValidationException.class);

    router
        .solveAllSchemaReferences(schema)
        .setHandler(testContext.succeeding(s -> {
          testContext.verify(() -> {
            assertThat(s).isSameAs(schema);
            assertThat(schema)
                .isSync();
            assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
                .doesNotThrowAnyException();
            assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
                .isInstanceOf(ValidationException.class);
            assertThat(router)
                .containsCachedSchemasWithXIds("main", "hello_prop", "hello_def");
          });
          testContext.completeNow();
        }));
  }

  @Test
  public void testCircularRefs(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = buildBaseUri("mutable_schema_test", "circular.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject()))
        .isInstanceOf(NoSyncValidationException.class);

    router
        .solveAllSchemaReferences(schema)
        .setHandler(testContext.succeeding(s -> {
          testContext.verify(() -> {
            assertThat(s).isSameAs(schema);
            assertThat(schema)
                .isSync();
            assertThatCode(() -> schema.validateSync(new JsonObject()
                .put("a", new JsonObject())
                .put("c", new JsonArray().add(1).add(new JsonObject()))
                .put("b", new JsonObject().put("sub-a", 1).put("sub-b", new JsonArray().add(1).add(new JsonObject().put("c", new JsonArray().add(1)))))
            )).doesNotThrowAnyException();
            assertThatThrownBy(() -> schema.validateSync(new JsonObject()
                .put("a", new JsonObject())
                .put("c", new JsonArray().add(1).add(new JsonObject()))
                .put("b", new JsonObject().put("sub-a", 1).put("sub-b", new JsonArray().add(1).add(new JsonObject().put("c", new JsonArray().addNull()))))
            )).isInstanceOf(ValidationException.class);
            assertThat(router)
                .containsCachedSchemasWithXIds("main", "a", "b", "c", "c-items", "sub", "sub-a", "sub-b", "sub-b-items");
          });
          testContext.completeNow();
        }));
  }

}
