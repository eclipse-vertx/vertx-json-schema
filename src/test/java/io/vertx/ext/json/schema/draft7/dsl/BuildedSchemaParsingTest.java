package io.vertx.ext.json.schema.draft7.dsl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.ext.json.schema.draft7.dsl.Keywords.exclusiveMaximum;
import static io.vertx.ext.json.schema.draft7.dsl.Keywords.multipleOf;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;

@ExtendWith(VertxExtension.class)
public class BuildedSchemaParsingTest {

  SchemaRouter router;
  SchemaParser parser;

  @BeforeEach
  public void setUp(Vertx vertx) {
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft7SchemaParser.create(router);
  }

  @Test
  public void testCircularTreeDeclaration(VertxTestContext testContext) {
    Schema schema =
        objectSchema()
            .alias("root_object")
            .requiredProperty("value",
                intSchema()
                    .with(exclusiveMaximum(20d), multipleOf(2d))
            )
            .property("leftChild", refToAlias("root_object"))
            .property("rightChild", refToAlias("root_object"))
            .build(parser);
    testContext.assertComplete(schema.validateAsync(
        new JsonObject().put("value", 6).put("leftChild", new JsonObject().put("value", 2))
    )).setHandler(v -> testContext.completeNow());
  }

  @Test
  public void testRelativeFileResolution(VertxTestContext testContext) {
    Schema schema = ref(JsonPointer.fromURI(URI.create("ref_test/sample.json"))).build(parser);

    testContext.assertComplete(schema.validateAsync("")).setHandler(v -> {
      testContext.verify(() -> {
        assertThat(router).containsOnlyOneCachedSchemaWithXId("main");
        assertThat(router).containsOnlyOneCachedSchemaWithXId("sub1");
      });
      testContext.completeNow();
    });
  }

}
