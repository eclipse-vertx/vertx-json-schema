package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface ValidationError {
  String keyword();
  String keywordLocation();
  String instanceLocation();
  String error();
}
