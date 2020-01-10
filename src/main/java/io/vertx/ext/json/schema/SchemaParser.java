package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.common.ValidatorFactory;
import io.vertx.ext.json.schema.draft7.Draft7SchemaParser;
import io.vertx.ext.json.schema.openapi3.OpenAPI3SchemaParser;

import java.util.function.Predicate;

/**
 * Parse a Json Schema. The parser can be extended to support custom keywords using {@link this#withValidatorFactory(ValidatorFactory)}
 *
 */
@VertxGen
public interface SchemaParser {

  /**
   * Build a schema from provided json assigning a random scope. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param jsonSchema JSON representing the schema
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parse(JsonObject jsonSchema);

  /**
   * Build a schema from provided json. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param jsonSchema JSON representing the schema
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parse(JsonObject jsonSchema, JsonPointer schemaPointer);

  /**
   * Builds a true of false schema assigning a random scope
   *
   * @param jsonSchema JSON representing the schema
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parse(Boolean jsonSchema);

  /**
   * Builds a true of false schema
   *
   * @param jsonSchema JSON representing the schema
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parse(Boolean jsonSchema, JsonPointer schemaPointer);

  /**
   * Build a schema from provided unparsed json assigning a random scope. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param unparsedJson Unparsed JSON representing the schema.
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parseFromString(String unparsedJson);

  /**
   * Build a schema from provided unparsed json. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param unparsedJson Unparsed JSON representing the schema.
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException If schema is invalid
   */
  Schema parseFromString(String unparsedJson, JsonPointer schemaPointer);

  /**
   * Get schema router registered to this schema parser
   *
   * @return
   */
  SchemaRouter getSchemaRouter();

  /**
   * Add a {@link ValidatorFactory} to this schema parser to support custom keywords
   *
   * @param factory new factory
   * @return a reference to this
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  SchemaParser withValidatorFactory(ValidatorFactory factory);

  /**
   * Add a custom format validator
   *
   * @param formatName format name
   * @param predicate predicate for the new format
   * @return a reference to this
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  SchemaParser withStringFormatValidator(String formatName, Predicate<String> predicate);

  /**
   * Create a new {@link SchemaParser} for OpenAPI schemas
   *
   * @param router
   * @return
   */
  static SchemaParser createOpenAPI3SchemaParser(SchemaRouter router) {
    return OpenAPI3SchemaParser.create(router);
  }

  /**
   * Create a new {@link SchemaParser} for Json Schema Draft-7 schemas
   *
   * @param router
   * @return
   */
  static SchemaParser createDraft7SchemaParser(SchemaRouter router) {
    return Draft7SchemaParser.create(router);
  }
}
