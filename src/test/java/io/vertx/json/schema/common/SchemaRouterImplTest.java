package io.vertx.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static io.vertx.json.schema.TestUtils.loadJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(VertxExtension.class)
public class SchemaRouterImplTest {

  @Test
  public void resolveCachedSchemaInJsonContainingSchemasWithRelativeRef(Vertx vertx) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.addJson(baseJsonUri, loadJson(baseJsonUri));

    Schema schema = schemaRouter.resolveCachedSchema(
      JsonPointer.from("/components/schemas/TrafficLight"),
      JsonPointer.fromURI(baseJsonUri),
      schemaParser
    );

    MyAssertions.assertThat(schema)
      .isNotNull();
    assertThat(schema.getJson())
      .isInstanceOf(JsonObject.class);

    MyAssertions.assertThat((JsonObject) schema.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for TrafficLight");
  }

  @Test
  public void resolveCachedSchemaInJsonContainingSchemasWithAbsoluteRef(Vertx vertx) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.addJson(baseJsonUri, loadJson(baseJsonUri));

    Schema schema = schemaRouter.resolveCachedSchema(
      JsonPointer.fromURI(URIUtils.replaceFragment(baseJsonUri, "/components/schemas/TrafficLight")),
      JsonPointer.create(),
      schemaParser
    );

    MyAssertions.assertThat(schema)
      .isNotNull();
    assertThat(schema.getJson())
      .isInstanceOf(JsonObject.class);

    MyAssertions.assertThat((JsonObject) schema.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for TrafficLight");
  }

  @Test
  public void resolveMultipleCachedSchemaInJsonContainingSchemasWithRelativeRef(Vertx vertx) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.addJson(baseJsonUri, loadJson(baseJsonUri));

    Schema trafficLight = schemaRouter.resolveCachedSchema(
      JsonPointer.from("/components/schemas/TrafficLight"),
      JsonPointer.fromURI(baseJsonUri),
      schemaParser
    );

    MyAssertions.assertThat(trafficLight)
      .isNotNull();
    assertThat(trafficLight.getJson())
      .isInstanceOf(JsonObject.class);
    MyAssertions.assertThat((JsonObject) trafficLight.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for TrafficLight");

    Schema roadLayout = schemaRouter.resolveCachedSchema(
      JsonPointer.from("/components/schemas/RoadLayout"),
      JsonPointer.fromURI(baseJsonUri),
      schemaParser
    );
    MyAssertions.assertThat(roadLayout)
      .isNotNull();
    assertThat(roadLayout.getJson())
      .isInstanceOf(JsonObject.class);
    MyAssertions.assertThat((JsonObject) roadLayout.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for RoadLayout");
  }

  @Test
  public void resolveMultipleCachedSchemaInJsonContainingSchemasWithAbsoluteRef(Vertx vertx) throws Exception {
    final URI baseJsonUri = buildBaseUri("openapi.json");
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    schemaRouter.addJson(baseJsonUri, loadJson(baseJsonUri));

    Schema trafficLight = schemaRouter.resolveCachedSchema(
      JsonPointer.fromURI(URIUtils.replaceFragment(baseJsonUri, "/components/schemas/TrafficLight")),
      JsonPointer.create(),
      schemaParser
    );
    MyAssertions.assertThat(trafficLight)
      .isNotNull();
    assertThat(trafficLight.getJson())
      .isInstanceOf(JsonObject.class);
    MyAssertions.assertThat((JsonObject) trafficLight.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for TrafficLight");

    Schema roadLayout = schemaRouter.resolveCachedSchema(
      JsonPointer.fromURI(URIUtils.replaceFragment(baseJsonUri, "/components/schemas/RoadLayout")),
      JsonPointer.create(),
      schemaParser
    );
    MyAssertions.assertThat(roadLayout)
      .isNotNull();
    assertThat(roadLayout.getJson())
      .isInstanceOf(JsonObject.class);
    MyAssertions.assertThat((JsonObject) roadLayout.getJson())
      .extractingKey("title")
      .isEqualTo("Root Type for RoadLayout");
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
          MyAssertions.assertThat(schema)
            .isNotNull();
          assertThat(schema.getJson())
            .isInstanceOf(JsonObject.class);
          MyAssertions.assertThat((JsonObject) schema.getJson())
            .extractingKey("title")
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
          MyAssertions.assertThat(schema)
            .isNotNull();
          assertThat(schema.getJson())
            .isInstanceOf(JsonObject.class);
          MyAssertions.assertThat((JsonObject) schema.getJson())
            .extractingKey("title")
            .isEqualTo("Root Type for TrafficLight");
        });
        testContext.completeNow();
      });
  }

}
