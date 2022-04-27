package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.validator.impl.SchemaValidatorImpl;

import java.util.Collections;

@VertxGen
public interface SchemaValidator {

  static SchemaValidator create(Schema schema, JsonSchemaOptions options) {
    return new SchemaValidatorImpl(schema, options, Collections.emptyMap());
  }

  OutputUnit validate(Object instance) throws SchemaException;
}
