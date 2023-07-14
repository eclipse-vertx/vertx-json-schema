package io.vertx.json.schema.impl;

import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.StringFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.vertx.json.schema.Draft.DRAFT201909;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.json.schema.common.dsl.StringFormat.BYTE;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaValidatorImplTest {
  private static final JsonSchemaOptions DUMMY_OPTIONS =
    new JsonSchemaOptions().setBaseUri("app://").setDraft(DRAFT201909);

  static Stream<Arguments> testStringSchema() {
    return Stream.of(
      Arguments.of(BYTE, "VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIFN0cmluZw==", true),
      Arguments.of(BYTE, "sdFd/+==", true),
      Arguments.of(BYTE, "VG=", false),
      Arguments.of(BYTE, "VG =", false),
      Arguments.of(BYTE, "%sample string value", false)
    );
  }

  @ParameterizedTest(name = "{index} With keyword format: {0} and value {1}")
  @MethodSource
  void testStringSchema(StringFormat formatValue, Object value, boolean isValid) {
    JsonSchema schema = JsonSchema.of(stringSchema().with(Keywords.format(formatValue)).toJson());
    SchemaValidatorImpl validator = new SchemaValidatorImpl(schema, DUMMY_OPTIONS);
    assertThat(validator.validate(value).getValid()).isEqualTo(isValid);
  }
}
