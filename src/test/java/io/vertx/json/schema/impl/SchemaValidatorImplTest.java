package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.NumberKeyword;
import io.vertx.json.schema.common.dsl.StringFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import static io.vertx.json.schema.Draft.DRAFT201909;
import static io.vertx.json.schema.common.dsl.Schemas.*;
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

  static Stream<Arguments> testNumberSchema() {
    return Stream.of(
      Arguments.of("Invalid Double", numberSchema().toJson().put("format", "double"), new BigDecimal("99999999.999"), false),
      Arguments.of("Invalid Float", numberSchema().toJson().put("format", "float"), Double.MAX_VALUE, false),
      Arguments.of("Invalid int32", intSchema().toJson().put("format", "int32"), Long.MAX_VALUE, false),
      Arguments.of("Invalid int64", intSchema().toJson().put("format", "int64"), new BigInteger("99999999999999999999999"), false),
      Arguments.of("Valid Double", numberSchema().toJson().put("format", "double"), Double.MAX_VALUE, true),
      Arguments.of("Valid Float", numberSchema().toJson().put("format", "float"), Float.MAX_VALUE, true),
      Arguments.of("Valid int32", intSchema().toJson().put("format", "int32"), Integer.MAX_VALUE, true),
      Arguments.of("Valid int64", intSchema().toJson().put("format", "int64"), Long.MAX_VALUE, true),
      Arguments.of("Valid int64 with short", intSchema().toJson().put("format", "int64"), Short.MAX_VALUE, true),
      Arguments.of("Valid int64 with byte", intSchema().toJson().put("format", "int64"), Byte.MAX_VALUE, true),
      Arguments.of("Valid int32 with short", intSchema().toJson().put("format", "int32"), Short.MAX_VALUE, true),
      Arguments.of("Valid int32 with byte", intSchema().toJson().put("format", "int32"), Byte.MAX_VALUE, true),
      Arguments.of("Valid float with smaller value casted as a double", numberSchema().toJson().put("format", "float"), 1.4d, false),
      Arguments.of("Valid double with smaller value", numberSchema().toJson().put("format", "double"), 123.4f, true)
      );
  }

  @ParameterizedTest(name = "{index} With keyword format: {0} and value {1}")
  @MethodSource
  void testStringSchema(StringFormat formatValue, Object value, boolean isValid) {
    JsonSchema schema = JsonSchema.of(stringSchema().with(Keywords.format(formatValue)).toJson());
    SchemaValidatorImpl validator = new SchemaValidatorImpl(schema, DUMMY_OPTIONS);
    assertThat(validator.validate(value).getValid()).isEqualTo(isValid);
  }

  @ParameterizedTest(name = "{0} with the value {2}")
  @MethodSource
  void testNumberSchema(String keyword, JsonObject objSchema, Object value, boolean isValid) {
    JsonSchema schema = JsonSchema.of(objSchema);
    SchemaValidatorImpl validator = new SchemaValidatorImpl(schema, DUMMY_OPTIONS);
    assertThat(validator.validate(value).getValid()).isEqualTo(isValid);
  }

}
