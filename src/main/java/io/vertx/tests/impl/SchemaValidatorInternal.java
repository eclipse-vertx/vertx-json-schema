package io.vertx.tests.impl;

import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.Validator;

public interface SchemaValidatorInternal extends Validator {

  JsonSchema schema();
}
