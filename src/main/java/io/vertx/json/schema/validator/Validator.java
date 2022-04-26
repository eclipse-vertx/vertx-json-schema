package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.validator.impl.ValidatorImpl;

@VertxGen
public interface Validator {

  static <T> Validator create(Schema schema, ValidatorOptions options) {
    return new ValidatorImpl(schema, options);
  }

  @Fluent Validator addSchema(Schema schema);

  @Fluent Validator addSchema(String uri, Schema schema);

  OutputUnit validate(Object instance);
}
