package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.validator.impl.SchemaRepositoryImpl;

/**
 * @TODO: A repository is a holder of dereferenced schemas, it can be used to create validator instances for a specific schema.
 */
@VertxGen
public interface SchemaRepository {

  static SchemaRepository create(JsonSchemaOptions options) {
    return new SchemaRepositoryImpl(options);
  }

  @Fluent
  SchemaRepository dereference(Schema schema) throws SchemaException;

  @Fluent
  SchemaRepository dereference(String uri, Schema schema) throws SchemaException;

  SchemaValidator validator(Schema schema);
  SchemaValidator validator(Schema schema, JsonSchemaOptions options);
}
