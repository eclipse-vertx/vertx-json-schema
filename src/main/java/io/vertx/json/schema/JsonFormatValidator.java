package io.vertx.json.schema;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface JsonFormatValidator {

  /**
   * The default validator which performs a NOOP.
   */
  JsonFormatValidator DEFAULT_VALIDATOR = (instanceType, format, instance) -> null;

  /**
   * @param instanceType The type of the related instance
   * @param format       The format specified in the schema for the current object instance.
   * @param instance     The current object instance that is currently being validated.
   * @return Any string if there are any format validation errors, null if there are no validation errors.
   */
  String validateFormat(String instanceType, String format, Object instance);

}
