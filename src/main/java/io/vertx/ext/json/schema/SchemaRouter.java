package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.common.SchemaRouterImpl;

import java.net.URI;
import java.util.List;

/**
 * Represents a pool where parsed schemas are addressed and cached. <br/>
 *
 * It also contains a cache of {@link JsonObject} including on top or inner level some json schemas that could eventually parsed later.<br/>
 *
 * You should not share this object between different threads
 *
 * @author slinkydeveloper
 */
@VertxGen
public interface SchemaRouter {

  /**
   * Resolve cached schema based on refPointer. If a schema isn't cached, it returns null
   *
   * @param refPointer
   * @param schemaScope
   * @param parser
   * @return the resolved schema, or null if no schema was found
   * @throws SchemaException If was found an unparsed schema that is an invalid json schema
   */
  @Nullable Schema resolveCachedSchema(JsonPointer refPointer, JsonPointer schemaScope, SchemaParser parser) throws SchemaException;

  /**
   * Resolve $ref. <br/>
   * This method tries to resolve schema from local cache. If it's not found, it solve external references.
   * It can solve external references on filesystem and remote references using http.
   * When you pass a relative reference without protocol, it tries to infer the absolute path from scope and cached schemas <br/>
   * Returns a future that can contain Schema or be null or can fail with a {@link SchemaException} or an {@link IllegalArgumentException}
   *
   * @param pointer
   * @param scope
   * @param schemaParser
   * @return a succeeded future that contains the resolved {@link Schema} or failed with a {@link SchemaException} or an {@link IllegalArgumentException}
   */
  Future<Schema> resolveRef(JsonPointer pointer, JsonPointer scope, SchemaParser schemaParser);

  /**
   * Add a parsed schema to local cache. This method is automatically called by {@link SchemaParser} when a new schema is parsed
   *
   * @param schema
   * @return a reference to this
   */
  @Fluent
  SchemaRouter addSchema(Schema schema);

  /**
   * Add one or more json documents including schemas on top or inner levels. This method doesn't trigger the schema parsing<br/>
   *
   * You can use this schema if you have externally loaded some json document and you want to register to the schema router.
   * You can later parse and retrieve a schema from this json structure using {@link this#resolveCachedSchema(JsonPointer, JsonPointer, SchemaParser)},
   * providing the correct {@code refPointer}
   *
   * @param uri
   * @param object
   * @return a reference to this
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  SchemaRouter addJson(URI uri, JsonObject object);

  /**
   * Add one or more json documents including schemas on top or inner levels. This method doesn't trigger the schema parsing<br/>
   *
   * You can use this schema if you have externally loaded some json document and you want to register to the schema router.
   * You can later parse and retrieve a schema from this json structure using {@link this#resolveCachedSchema(JsonPointer, JsonPointer, SchemaParser)},
   * providing the correct {@code refPointer}
   *
   * @param uri
   * @param object
   * @return a reference to this
   */
  default SchemaRouter addJson(String uri, JsonObject object) {
    return addJson(URI.create(uri), object);
  }

  /**
   * @return a list of all registered schemas
   */
  List<Schema> registeredSchemas();

  /**
   * Deeply resolve all references of the provided {@code schema}
   *
   * @param schema
   * @return returns a succeeded future with same instance of provided schema, or a failed schema if something went wrong
   */
  @GenIgnore //TODO waiting for codegen future update
  Future<Schema> solveAllSchemaReferences(Schema schema);

  /**
   * Create a new {@link SchemaRouter}
   *
   * @param vertx
   * @param schemaRouterOptions
   * @return
   */
  static SchemaRouter create(Vertx vertx, SchemaRouterOptions schemaRouterOptions) {
    return new SchemaRouterImpl(vertx.createHttpClient(), vertx.fileSystem(), schemaRouterOptions);
  }

  /**
   * Create a new {@link SchemaRouter}
   *
   * @param client
   * @param fs
   * @param schemaRouterOptions
   * @return
   */
  static SchemaRouter create(HttpClient client, FileSystem fs, SchemaRouterOptions schemaRouterOptions) {
    return new SchemaRouterImpl(client, fs, schemaRouterOptions);
  }

}
