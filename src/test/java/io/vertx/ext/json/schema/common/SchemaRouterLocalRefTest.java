package io.vertx.ext.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.openapi3.OpenAPI3SchemaParser;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class SchemaRouterLocalRefTest {

  public SchemaParserInternal parser;
  public SchemaRouter router;

  @BeforeEach
  public void setUp(Vertx vertx) throws Exception {
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = OpenAPI3SchemaParser.create(router);
  }

  @Test
  public void absoluteLocalRef(VertxTestContext context) {
    URI sampleURI = buildBaseUri("ref_test", "sample.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, buildBaseUri("ref_test", "test_1.json"));
    mainSchema.validateAsync("").setHandler(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void relativeLocalRef(VertxTestContext context) {
    URI sampleURI = URI.create("./sample.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, Paths.get(".","src", "test", "resources", "ref_test", "test_2.json").toUri());
    mainSchema.validateAsync("").setHandler(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void relativeLocalRefFromResources(VertxTestContext context) throws URISyntaxException {
    URI sampleURI = getClass().getResource("/ref_test/sample.json").toURI();
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, sampleURI.resolve("test_1.json"));
    mainSchema.validateAsync("").setHandler(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void jarURIRelativization(VertxTestContext context) throws URISyntaxException {
    URI sampleURI = getClass().getClassLoader().getResource("sample_in_jar.json").toURI();
    URI replaced1 = URIUtils.resolvePath(sampleURI, "empty_in_jar.json");
    URI replaced2 = URIUtils.resolvePath(sampleURI, "./empty_in_jar.json");
    context.verify(() -> {
      try {
        assertThat(getClass().getClassLoader().getResource("empty_in_jar.json").toURI()).isEqualTo(replaced1);
        assertThat(getClass().getClassLoader().getResource("empty_in_jar.json").toURI()).isEqualTo(replaced2);
      } catch (URISyntaxException e) {
        fail("Wrong URI syntax", e);
      }
    });
    context.completeNow();
  }

  @Test
  public void relativeLocalRefFromClassLoader(VertxTestContext context) throws URISyntaxException {
    URI sampleURI = getClass().getClassLoader().getResource("sample_in_jar.json").toURI();
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, URIUtils.resolvePath(sampleURI, "test_1.json"));
    mainSchema.validateAsync("").setHandler(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void localRecursiveRef(VertxTestContext context) {
    Checkpoint check = context.checkpoint(2);

    URI sampleURI = buildBaseUri("ref_test", "person_recursive.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, buildBaseUri("ref_test", "test_1.json"));
    mainSchema.validateAsync(new JsonObject()
      .put("name", "Francesco")
      .put("surname", "Guardiani")
      .put("id_card", "ABC")
      .put("father", new JsonObject()
        .put("name", "Pietro")
        .put("surname", "Guardiani")
        .put("id_card", "XYZ")
      )
    ).setHandler(context.succeeding(o -> check.flag()));
    mainSchema.validateAsync(new JsonObject()
      .put("name", "Francesco")
      .put("surname", "Guardiani")
      .put("id_card", "ABC")
      .put("father", new JsonObject()
        .put("name", "Pietro")
        .put("surname", "Guardiani") // No id card!
      )
    ).setHandler(context.failing(o -> check.flag()));
  }

}
