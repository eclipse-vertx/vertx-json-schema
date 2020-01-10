package io.vertx.ext.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaRouter;
import io.vertx.ext.json.schema.SchemaRouterOptions;
import io.vertx.ext.json.schema.openapi3.OpenAPI3SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.common.URIUtils.createJsonPointerFromURI;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchemaRouterIdTest {

  public SchemaParserInternal parser;
  public SchemaRouter schemaRouter;

  @BeforeAll
  public void setUp(Vertx vertx) {
    schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = OpenAPI3SchemaParser.create(schemaRouter);
  }

  @Test
  public void testNoIdKeyword() throws Exception {
    URI baseURI = buildBaseUri("id_test", "no_id_keyword.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create(), basePointer, parser).hasXIdEqualsTo("main");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create(), basePointer, parser).hasXIdEqualsTo("main");
    assertThat(schemaRouter).canResolveSchema(basePointer, basePointer, parser).hasXIdEqualsTo("main");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("allOf").append("0"), basePointer, parser).hasXIdEqualsTo("allOf_0");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("allOf").append("1"), basePointer, parser).hasXIdEqualsTo("allOf_1");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("anyOf").append("0"), basePointer, parser).hasXIdEqualsTo("anyOf_0");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("anyOf").append("1"), basePointer, parser).hasXIdEqualsTo("anyOf_1");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("oneOf").append("0"), basePointer, parser).hasXIdEqualsTo("oneOf_0");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("oneOf").append("1"), basePointer, parser).hasXIdEqualsTo("oneOf_1");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("not"), basePointer, parser).hasXIdEqualsTo("not");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("prop_1"), basePointer, parser).hasXIdEqualsTo("prop_1");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("prop_2"), basePointer, parser).hasXIdEqualsTo("prop_2");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("patternProperties").append("^a"), basePointer, parser).hasXIdEqualsTo("pattern_prop_1");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("additionalProperties"), basePointer, parser).hasXIdEqualsTo("additional_prop");
  }

  @Test
  public void testIdURNKeywordFromBaseScope() throws Exception {
    URI baseURI = buildBaseUri("id_test", "id_urn_keyword.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);

    assertThat(schemaRouter).canResolveSchema(basePointer.copy().append("properties").append("prop_1"), basePointer, parser).hasXIdEqualsTo("prop_1");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("urn:uuid:590e34ae-8e3d-4bdf-a748-beff72654d0e")), basePointer, parser).hasXIdEqualsTo("prop_1");
    assertThat(schemaRouter).canResolveSchema(basePointer.copy().append("properties").append("prop_2"), basePointer, parser).hasXIdEqualsTo("prop_2");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("urn:uuid:77ed19ca-1127-42dd-8194-3e48661ce672")), basePointer, parser).hasXIdEqualsTo("prop_2");
    assertThat(schemaRouter).canResolveSchema(basePointer.copy().append("properties").append("prop_2").append("not"), basePointer, parser).hasXIdEqualsTo("not");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("urn:uuid:77ed19ca-1127-42dd-8194-3e48661ce672")).append("not"), basePointer, parser).hasXIdEqualsTo("not");
  }

  @Test
  public void testIdURNKeywordFromInnerScope() throws Exception {
    URI baseURI = buildBaseUri("id_test", "id_urn_keyword.json");
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);
    JsonPointer scope = schemaRouter.resolveCachedSchema(createJsonPointerFromURI(URI.create("urn:uuid:77ed19ca-1127-42dd-8194-3e48661ce672")), createJsonPointerFromURI(baseURI), parser).getScope();

    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("urn:uuid:77ed19ca-1127-42dd-8194-3e48661ce672")).append("not"), scope, parser).hasXIdEqualsTo("not");
  }

    /*

   # (document root)
         http://example.com/root.json
         http://example.com/root.json#

   #/definitions/A
         http://example.com/root.json#foo
         http://example.com/root.json#/definitions/A

   #/definitions/B
         http://example.com/other.json
         http://example.com/other.json#
         http://example.com/root.json#/definitions/B

   #/definitions/B/definitions/X
         http://example.com/other.json#bar
         http://example.com/other.json#/definitions/X
         http://example.com/root.json#/definitions/B/definitions/X

   #/definitions/B/definitions/Y
         http://example.com/t/inner.json
         http://example.com/t/inner.json#
         http://example.com/other.json#/definitions/Y
         http://example.com/root.json#/definitions/B/definitions/Y

   #/definitions/C
         urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f
         urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f#
         http://example.com/root.json#/definitions/C
     */

  @Test
  public void testRFCIDKeywordFromBaseScope() throws Exception {
    URI baseURI = buildBaseUri("id_test", "rfc_id_keyword.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create(), basePointer, parser).hasXIdEqualsTo("main");

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("A"), basePointer, parser).hasXIdEqualsTo("A");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("#foo")), basePointer, parser).hasXIdEqualsTo("A");

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("B"), basePointer, parser).hasXIdEqualsTo("B");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("http://example.com/other.json")), basePointer, parser).hasXIdEqualsTo("B");

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("B").append("properties").append("X"), basePointer, parser).hasXIdEqualsTo("X");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("#bar")), basePointer, parser).hasXIdEqualsTo("X");

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("B").append("properties").append("Y"), basePointer, parser).hasXIdEqualsTo("Y");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("http://example.com/t/inner.json")), basePointer, parser).hasXIdEqualsTo("Y");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("http://example.com/other.json")).append("properties").append("Y"), basePointer, parser).hasXIdEqualsTo("Y");

    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("C"), basePointer, parser).hasXIdEqualsTo("C");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("http://example.com/root.json")).append("properties").append("C"), basePointer, parser).hasXIdEqualsTo("C");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f")), basePointer, parser).hasXIdEqualsTo("C");

  }

  @Test
  public void testRFCIDKeywordFromInnerScope() throws Exception {
    URI baseURI = buildBaseUri("id_test", "rfc_id_keyword.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);
    JsonPointer scope = schemaRouter.resolveCachedSchema(createJsonPointerFromURI(URI.create("http://example.com/other.json")), basePointer, parser).getScope();

    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("#foo")), scope, parser).hasXIdEqualsTo("A");
    assertThat(schemaRouter).canResolveSchema(createJsonPointerFromURI(URI.create("#bar")), scope, parser).hasXIdEqualsTo("X");
    assertThat(schemaRouter).canResolveSchema(JsonPointer.create().append("properties").append("B").append("properties").append("Y"), scope, parser).hasXIdEqualsTo("Y");
  }

}
