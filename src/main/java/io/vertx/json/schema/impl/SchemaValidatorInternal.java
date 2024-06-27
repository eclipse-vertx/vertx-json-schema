package io.vertx.json.schema.impl;

import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.Validator;

public interface SchemaValidatorInternal extends Validator {

  JsonSchema schema();
}
