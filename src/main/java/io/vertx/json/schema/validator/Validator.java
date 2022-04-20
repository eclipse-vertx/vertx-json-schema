package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.validator.impl.ValidatorImpl;

@VertxGen
public interface Validator {

  static Validator create(Schema schema) {
    return create(schema, new ValidatorOptions());
  }

  static <T> Validator create(Schema schema, ValidatorOptions options) {
    return new ValidatorImpl(schema, options);
  }

  @Fluent Validator addSchema(Schema schema);

  @Fluent Validator addSchema(String uri, Schema schema);

  ValidationResult validate(Object instance);
}
