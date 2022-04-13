package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.JsonSchema;
import io.vertx.json.schema.validator.impl.ValidatorImpl;

@VertxGen
public interface Validator {

  static <T> Validator create(Schema<T> schema) {
    return create(schema, new ValidatorOptions());
  }

  static <T> Validator create(Schema<T> schema, ValidatorOptions options) {
    return new ValidatorImpl(schema, options);
  }

  @Fluent Validator addSchema(Schema<JsonObject> schema);

  default @Fluent Validator addSchema(Schema<JsonObject> schema, String id) {
    return addSchema(new JsonSchema(schema, id));
  }

  ValidationResult validate(Object instance);
}
