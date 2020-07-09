package io.vertx.ext.json.schema.draft201909;

import io.vertx.core.Vertx;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.BaseIntegrationTest;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaParser;
import io.vertx.ext.json.schema.SchemaRouterOptions;
import io.vertx.ext.json.schema.common.SchemaRouterImpl;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public class Draft201909IntegrationTest extends BaseIntegrationTest {

  @Override
  public Map.Entry<SchemaParser, Schema> buildSchemaFunction(Vertx vertx, Object schema, String testFileName) {
    Draft201909SchemaParser parser = Draft201909SchemaParser.create(new SchemaRouterImpl(vertx.createHttpClient(), vertx.fileSystem(), new SchemaRouterOptions()));
    Schema s = parser.parse(schema, Paths.get(this.getTckPath() + "/" + testFileName + ".json").toAbsolutePath().toUri());
    return new AbstractMap.SimpleImmutableEntry<>(parser, s);
  }

  @Override
  public Stream<String> getTestFiles() {
    return Stream.of(
      "additionalItems",
      "additionalProperties",
      "allOf",
      "anchor",
      "anyOf",
      "boolean_schema",
      "const",
      "contains",
      "default",
      "defs",
      "dependentRequired",
      "dependentSchemas",
      "enum",
      "exclusiveMaximum",
      "exclusiveMinimum",
      "format",
      "id",
      "if-then-else",
      "items",
      "maxContains",
      "maximum",
      "maxItems",
      "maxLength",
      "maxProperties",
      "minContains",
      "minimum",
      "minItems",
      "minLength",
      "minProperties",
      "multipleOf",
      "not",
      "oneOf",
      "pattern",
      "patternProperties",
      "properties",
      "propertyNames",
      "ref",
      "refRemote",
      "required",
      "type",
      "unevaluatedItems",
      "unevaluatedProperties",
      "uniqueItems"
    );
  }

  @Override
  public Path getTckPath() {
    return Paths.get("src", "test", "resources", "tck", "draft2019-09");
  }

  @Override
  public Path getRemotesPath() {
    return Paths.get("src", "test", "resources", "tck", "draft2019-09", "remotes");
  }

  @Override
  public boolean skipSyncCheck(Schema schema) {
    Object res = JsonPointer.create().append("$ref").queryJson(schema.getJson());
    if (res instanceof String) {
      return "json-schema.org".equals(URI.create((String) res).getHost());
    }
    return false;
  }
}
