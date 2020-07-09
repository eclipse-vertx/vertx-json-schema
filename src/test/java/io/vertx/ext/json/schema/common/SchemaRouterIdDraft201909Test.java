package io.vertx.ext.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaRouter;
import io.vertx.ext.json.schema.SchemaRouterOptions;
import io.vertx.ext.json.schema.draft201909.Draft201909SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchemaRouterIdDraft201909Test {

  public SchemaParserInternal parser;
  public SchemaRouter schemaRouter;

  @BeforeAll
  public void setUp(Vertx vertx) {
    schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft201909SchemaParser.create(schemaRouter);
  }

  /*

    From https://json-schema.org/draft/2019-09/json-schema-core.html#rfc.appendix.A

    # (document root)

        canonical absolute-URI (and also base URI)
            https://example.com/root.json
        canonical URI with pointer fragment
            https://example.com/root.json#

    #/$defs/A

        base URI
            https://example.com/root.json
        canonical URI with plain fragment
            https://example.com/root.json#foo
        canonical URI with pointer fragment
            https://example.com/root.json#/$defs/A

    #/$defs/B

        base URI
            https://example.com/other.json
        canonical URI with pointer fragment
            https://example.com/other.json#
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B

    #/$defs/B/$defs/X

        base URI
            https://example.com/other.json
        canonical URI with plain fragment
            https://example.com/other.json#bar
        canonical URI with pointer fragment
            https://example.com/other.json#/$defs/X
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B/$defs/X

    #/$defs/B/$defs/Y

        base URI
            https://example.com/t/inner.json
        canonical URI with plain fragment
            https://example.com/t/inner.json#bar
        canonical URI with pointer fragment
            https://example.com/t/inner.json#
        non-canonical URI with fragment relative to other.json
            https://example.com/other.json#/$defs/Y
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B/$defs/Y

    #/$defs/C

        base URI
            urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f
        canonical URI with pointer fragment
            urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f#
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/C

   */

  @Test
  public void testDraft201909AppendixA() throws Exception {
    URI baseURI = buildBaseUri("id_test", "rfc_201909_appendix_a.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);

    assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), basePointer, parser)
      .hasXIdEqualsTo("root");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json", basePointer, parser)
      .hasXIdEqualsTo("root");

    assertThat(schemaRouter)
      .canResolveSchema("#/$defs/A", basePointer, parser)
      .hasXIdEqualsTo("A");
    assertThat(schemaRouter)
      .canResolveSchema("#foo", basePointer, parser)
      .hasXIdEqualsTo("A");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/A", basePointer, parser)
      .hasXIdEqualsTo("A");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#foo", basePointer, parser)
      .hasXIdEqualsTo("A");

    assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B", basePointer, parser)
      .hasXIdEqualsTo("B");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json", basePointer, parser)
      .hasXIdEqualsTo("B");
    assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), JsonPointer.fromURI(URI.create("https://example.com/other.json")), parser)
      .hasXIdEqualsTo("B");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B", basePointer, parser)
      .hasXIdEqualsTo("B");

    assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");
    assertThat(schemaRouter)
      .cannotResolveSchema("#bar", basePointer, parser);
    assertThat(schemaRouter)
      .canResolveSchema("#bar", JsonPointer.fromURI(URI.create("https://example.com/other.json")), parser)
      .hasXIdEqualsTo("X");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#bar", basePointer, parser)
      .hasXIdEqualsTo("X");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");

    assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");
    assertThat(schemaRouter)
      .cannotResolveSchema("#bar", basePointer, parser);
    assertThat(schemaRouter)
      .canResolveSchema("#bar", JsonPointer.fromURI(URI.create("https://example.com/t/inner.json")), parser)
      .hasXIdEqualsTo("Y");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/t/inner.json#bar", basePointer, parser)
      .hasXIdEqualsTo("Y");
    assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), JsonPointer.fromURI(URI.create("https://example.com/t/inner.json")), parser)
      .hasXIdEqualsTo("Y");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");
    assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");

    assertThat(schemaRouter)
      .canResolveSchema("#/$defs/C", basePointer, parser)
      .hasXIdEqualsTo("C");
    assertThat(schemaRouter)
      .canResolveSchema("urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f", basePointer, parser)
      .hasXIdEqualsTo("C");
  }

}
