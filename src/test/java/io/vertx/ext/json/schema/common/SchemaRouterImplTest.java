package io.vertx.ext.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaParser;
import io.vertx.ext.json.schema.SchemaRouter;
import io.vertx.ext.json.schema.SchemaRouterOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(VertxExtension.class)
public class SchemaRouterImplTest {

  @Test
  public void resolveCachedSchemaInJsonContainingSchemas(Vertx vertx) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.addJson(baseJsonUri, loadJson(baseJsonUri));

    Schema schema = schemaRouter.resolveCachedSchema(
      JsonPointer.from("/components/schemas/TrafficLight"),
      JsonPointer.fromURI(baseJsonUri),
      schemaParser
    );

    assertThat(schema.getJson())
      .isInstanceOf(JsonObject.class);

    assertThat((JsonObject) schema.getJson())
      .extractingKey("description")
      .isEqualTo("Root Type for TrafficLight");
  }

  @Test
  public void resolveRefInJsonContainingSchemasWithRelativeRef(Vertx vertx, VertxTestContext testContext) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.resolveRef(
      JsonPointer.from("/components/schemas/TrafficLight"),
      JsonPointer.fromURI(baseJsonUri),
      schemaParser
    )
      .onFailure(testContext::failNow)
      .onSuccess(schema -> {
        testContext.verify(() -> {
          assertThat(schema)
            .isNotNull();
          assertThat(schema.getJson())
            .isInstanceOf(JsonObject.class);
          assertThat((JsonObject) schema.getJson())
            .extractingKey("description")
            .isEqualTo("Root Type for TrafficLight");
        });
        testContext.completeNow();
      });
  }

  @Test
  public void resolveRefInJsonContainingSchemasWithAbsoluteRef(Vertx vertx, VertxTestContext testContext) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.resolveRef(
      JsonPointer.fromURI(URIUtils.replaceFragment(baseJsonUri, "/components/schemas/TrafficLight")),
      JsonPointer.create(),
      schemaParser
    )
      .onFailure(testContext::failNow)
      .onSuccess(schema -> {
        testContext.verify(() -> {
          assertThat(schema)
            .isNotNull();
          assertThat(schema.getJson())
            .isInstanceOf(JsonObject.class);
          assertThat((JsonObject) schema.getJson())
            .extractingKey("title")
            .isEqualTo("Root Type for TrafficLight");
        });
        testContext.completeNow();
      });
  }

}
