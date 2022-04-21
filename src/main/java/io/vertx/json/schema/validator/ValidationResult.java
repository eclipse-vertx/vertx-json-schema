package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;

import java.util.List;

@VertxGen
public interface ValidationResult {
  boolean valid();
  List<ValidationError> errors();
}
