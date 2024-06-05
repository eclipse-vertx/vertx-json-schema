package io.vertx.json.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.json.schema.common.dsl.StringSchemaBuilder;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.vertx.json.schema.OutputFormat.Basic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidatorTest {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidates() {
    final Validator validator = Validator.create(
      JsonSchema.of(new JsonObject().put("type", "number")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT201909));

    assertThat(validator.validate(7).getValid())
      .isEqualTo(true);

    assertThat(validator.validate("hello world").getValid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testAddsSchema() {
    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setBaseUri("https://vertx.io")
          .setDraft(Draft.DRAFT201909));

    repository
      .dereference(JsonSchema.of(
        new JsonObject()
          .put("$id", "https://foo.bar/beep")
          .put("type", "boolean")))
      .dereference(JsonSchema.of(
        new JsonObject()
          .put("$id", "https://foo.bar/baz")
          .put("$ref", "/beep")));

    final Validator validator = repository.validator("https://foo.bar/baz");

    assertThat(validator.validate(true).getValid())
      .isEqualTo(true);
    assertThat(validator.validate("hello world").getValid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testAddsSchemaWithSpecifiedId() {
    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setBaseUri("https://vertx.io")
          .setDraft(Draft.DRAFT201909));

    repository
      .dereference(
        JsonSchema.of(
          new JsonObject()
            .put("$id", "https://foo.bar/beep")
            .put("type", "boolean")))
      .dereference(
        JsonSchema.of(
          new JsonObject()
            .put("$id", "https://foo.bar/baz")
            .put("$ref", "/beep")));

    final Validator validator = repository
      .validator("https://foo.bar/baz");


    assertThat(validator.validate(true).getValid())
      .isEqualTo(true);
    assertThat(validator.validate("hello world").getValid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidateAllArrayEntriesWithNestedErrors() {
    final Validator validator = Validator.create(
      JsonSchema.of(
        new JsonObject()
          .put("type", "array")
          .put("items", new JsonObject()
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("required", new JsonArray().add("name").add("email")))),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT201909)
        .setOutputFormat(Basic));

    final OutputUnit res = validator.validate(
      new JsonArray()
        .add(new JsonObject().put("name", "hello"))   // missing email
        .add(new JsonObject().put("email", "a@b.c"))  // missing name
    );

    assertThat(res.getValid()).isFalse();
    assertThat(res.getErrors().size()).isEqualTo(4);
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.MISSING_VALUE);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidateAllObjectPropertiesWithNestedErrors() {
    final Validator validator = Validator.create(
      JsonSchema.of(
        new JsonObject()
          .put("type", "object")
          .put("properties", new JsonObject()
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("number", new JsonObject().put("type", "number"))
            .put("required", new JsonArray().add("name").add("email").add("number")))),
      new JsonSchemaOptions()
        .setDraft(Draft.DRAFT201909)
        .setOutputFormat(Basic)
        .setBaseUri("https://vertx.io"));

    final OutputUnit res = validator.validate(
      new JsonObject()
        .put("name", "hello")
        .put("email", 5)      // invalid type
        .put("number", "Hello")   // invalid type
    );


    assertThat(res.getValid()).isFalse();
    assertThat(res.getErrors().size()).isEqualTo(4);
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.INVALID_VALUE);
  }

  @Test
  public void testRecursive() {
    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setDraft(Draft.DRAFT4)
          .setOutputFormat(Basic)
          .setBaseUri("https://github.com/eclipse-vertx"));

    repository.dereference(
      "http://json-schema.org/draft-04/schema",
      JsonSchema.of(new JsonObject("{\n" +
        "    \"id\": \"http://json-schema.org/draft-04/schema#\",\n" +
        "    \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
        "    \"description\": \"Core schema meta-schema\",\n" +
        "    \"definitions\": {\n" +
        "        \"schemaArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"items\": { \"$ref\": \"#\" }\n" +
        "        },\n" +
        "        \"positiveInteger\": {\n" +
        "            \"type\": \"integer\",\n" +
        "            \"minimum\": 0\n" +
        "        },\n" +
        "        \"positiveIntegerDefault0\": {\n" +
        "            \"allOf\": [ { \"$ref\": \"#/definitions/positiveInteger\" }, { \"default\": 0 } ]\n" +
        "        },\n" +
        "        \"simpleTypes\": {\n" +
        "            \"enum\": [ \"array\", \"boolean\", \"integer\", \"null\", \"number\", \"object\", \"string\" ]\n" +
        "        },\n" +
        "        \"stringArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"items\": { \"type\": \"string\" },\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        }\n" +
        "    },\n" +
        "    \"type\": \"object\",\n" +
        "    \"properties\": {\n" +
        "        \"id\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"$schema\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"title\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"description\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"default\": {},\n" +
        "        \"multipleOf\": {\n" +
        "            \"type\": \"number\",\n" +
        "            \"minimum\": 0,\n" +
        "            \"exclusiveMinimum\": true\n" +
        "        },\n" +
        "        \"maximum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMaximum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"minimum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMinimum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxLength\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minLength\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"pattern\": {\n" +
        "            \"type\": \"string\",\n" +
        "            \"format\": \"regex\"\n" +
        "        },\n" +
        "        \"additionalItems\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"items\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#\" },\n" +
        "                { \"$ref\": \"#/definitions/schemaArray\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"maxItems\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minItems\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"uniqueItems\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxProperties\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minProperties\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"required\": { \"$ref\": \"#/definitions/stringArray\" },\n" +
        "        \"additionalProperties\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"definitions\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"properties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"patternProperties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"dependencies\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": {\n" +
        "                \"anyOf\": [\n" +
        "                    { \"$ref\": \"#\" },\n" +
        "                    { \"$ref\": \"#/definitions/stringArray\" }\n" +
        "                ]\n" +
        "            }\n" +
        "        },\n" +
        "        \"enum\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        },\n" +
        "        \"type\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"items\": { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                    \"minItems\": 1,\n" +
        "                    \"uniqueItems\": true\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        \"format\": { \"type\": \"string\" },\n" +
        "        \"allOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"anyOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"oneOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"not\": { \"$ref\": \"#\" }\n" +
        "    },\n" +
        "    \"dependencies\": {\n" +
        "        \"exclusiveMaximum\": [ \"maximum\" ],\n" +
        "        \"exclusiveMinimum\": [ \"minimum\" ]\n" +
        "    },\n" +
        "    \"default\": {}\n" +
        "}\n")));

    final Validator validator = repository.validator("http://json-schema.org/draft-04/schema#");

    final OutputUnit res = validator.validate(
      new JsonObject("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}"));


    assertThat(res.getValid()).isTrue();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void testQuotedProps() {

    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setDraft(Draft.DRAFT4)
          .setOutputFormat(Basic)
          .setBaseUri("https://github.com/eclipse-vertx"));

    repository.dereference(
      "http://json-schema.org/draft-04/schema",
      JsonSchema.of(new JsonObject("{\n" +
        "    \"id\": \"http://json-schema.org/draft-04/schema#\",\n" +
        "    \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
        "    \"description\": \"Core schema meta-schema\",\n" +
        "    \"definitions\": {\n" +
        "        \"schemaArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"items\": { \"$ref\": \"#\" }\n" +
        "        },\n" +
        "        \"positiveInteger\": {\n" +
        "            \"type\": \"integer\",\n" +
        "            \"minimum\": 0\n" +
        "        },\n" +
        "        \"positiveIntegerDefault0\": {\n" +
        "            \"allOf\": [ { \"$ref\": \"#/definitions/positiveInteger\" }, { \"default\": 0 } ]\n" +
        "        },\n" +
        "        \"simpleTypes\": {\n" +
        "            \"enum\": [ \"array\", \"boolean\", \"integer\", \"null\", \"number\", \"object\", \"string\" ]\n" +
        "        },\n" +
        "        \"stringArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"items\": { \"type\": \"string\" },\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        }\n" +
        "    },\n" +
        "    \"type\": \"object\",\n" +
        "    \"properties\": {\n" +
        "        \"id\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"$schema\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"title\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"description\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"default\": {},\n" +
        "        \"multipleOf\": {\n" +
        "            \"type\": \"number\",\n" +
        "            \"minimum\": 0,\n" +
        "            \"exclusiveMinimum\": true\n" +
        "        },\n" +
        "        \"maximum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMaximum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"minimum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMinimum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxLength\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minLength\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"pattern\": {\n" +
        "            \"type\": \"string\",\n" +
        "            \"format\": \"regex\"\n" +
        "        },\n" +
        "        \"additionalItems\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"items\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#\" },\n" +
        "                { \"$ref\": \"#/definitions/schemaArray\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"maxItems\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minItems\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"uniqueItems\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxProperties\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minProperties\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"required\": { \"$ref\": \"#/definitions/stringArray\" },\n" +
        "        \"additionalProperties\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"definitions\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"properties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"patternProperties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"dependencies\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": {\n" +
        "                \"anyOf\": [\n" +
        "                    { \"$ref\": \"#\" },\n" +
        "                    { \"$ref\": \"#/definitions/stringArray\" }\n" +
        "                ]\n" +
        "            }\n" +
        "        },\n" +
        "        \"enum\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        },\n" +
        "        \"type\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"items\": { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                    \"minItems\": 1,\n" +
        "                    \"uniqueItems\": true\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        \"format\": { \"type\": \"string\" },\n" +
        "        \"allOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"anyOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"oneOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"not\": { \"$ref\": \"#\" }\n" +
        "    },\n" +
        "    \"dependencies\": {\n" +
        "        \"exclusiveMaximum\": [ \"maximum\" ],\n" +
        "        \"exclusiveMinimum\": [ \"minimum\" ]\n" +
        "    },\n" +
        "    \"default\": {}\n" +
        "}\n")));

    final Validator validator = repository.validator(
      JsonSchema.of(
        new JsonObject("{\"properties\":{\"foo\\\"bar\":{\"$ref\":\"#/definitions/foo%22bar\"}},\"definitions\":{\"foo\\\"bar\":{\"type\":\"number\"}}}")));

    final OutputUnit res = validator.validate(
      new JsonObject("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}"));

    assertThat(res.getValid()).isTrue();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void testWebValidationTest() {

    JsonObject schema = new JsonObject("{\"patternProperties\":{\"oneInteger\":{\"type\":\"integer\",\"$id\":\"urn:vertxschemas:2ea1a0cf-b474-43d0-8167-c2babeb52990\"},\"someIntegers\":{\"type\":\"array\",\"items\":{\"type\":\"integer\",\"$id\":\"urn:vertxschemas:dd85ddbb-e4b6-4cca-8f62-d635440de5b1\"},\"$id\":\"urn:vertxschemas:cfa6873d-c874-4537-a2aa-b6fc0a6ab061\"}},\"additionalProperties\":{\"type\":\"boolean\",\"$id\":\"urn:vertxschemas:135809d9-180b-40da-95a1-7e34caa32f80\"},\"type\":\"object\",\"properties\":{\"someNumbers\":{\"type\":\"array\",\"items\":{\"type\":\"number\",\"$id\":\"urn:vertxschemas:669a50a0-f3f1-4d1e-bb07-72ef8ac062a2\"},\"$id\":\"urn:vertxschemas:31baa881-3506-4b26-bd3b-94e3b9f6c4f9\"},\"oneNumber\":{\"type\":\"number\",\"$id\":\"urn:vertxschemas:43781a85-8ea3-45e3-8ac1-0056df19dde0\"}},\"$id\":\"urn:vertxschemas:ffcf420b-7600-4727-9bc1-4d3f541afcf9\"}");

    Validator validator = Validator.create(
      JsonSchema.of(schema),
      new JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("app://app.com").setOutputFormat(OutputFormat.Flag));

    validator.validate(new JsonObject());

  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidatorByRef() {
    final SchemaRepository repository = SchemaRepository
      .create(
        new JsonSchemaOptions()
          .setBaseUri("https://vertx.io")
          .setDraft(Draft.DRAFT201909)
          .setOutputFormat(OutputFormat.Basic));

    repository
      .dereference(JsonSchema.of(
        new JsonObject()
          .put("$id", "https://foo.bar/beep")
          .put("type", "boolean")))
      .dereference(JsonSchema.of(
        new JsonObject()
          .put("$id", "https://foo.bar/baz")
          .put("$ref", "/beep")));

    Validator validator = repository.validator("https://foo.bar/baz");

    assertThat(validator.validate(true).getValid())
      .isEqualTo(true);
    assertThat(validator.validate(true).getErrorType())
      .isEqualTo(OutputErrorType.NONE);
    assertThat(validator.validate("hello world").getValid())
      .isEqualTo(false);
    assertThat(validator.validate("hello world").getErrorType())
      .isEqualTo(OutputErrorType.INVALID_VALUE);
  }

  @Test
  public void testThrowErrorNoBaseUri() {
    JsonSchema dummySchema = JsonSchema.of(Schemas.stringSchema().toJson());
    NullPointerException exception = assertThrows(NullPointerException.class, () -> Validator.create(dummySchema,
      new JsonSchemaOptions()));
    assertThat(exception).hasMessage("'options.baseUri' cannot be null");
  }

  @Test
  public void testFormatValidatorIsPassed() {
    JsonSchemaOptions options =
      new JsonSchemaOptions().setBaseUri("https://vertx.io").setDraft(Draft.DRAFT202012).setOutputFormat(Basic);
    JsonSchema dummySchema = JsonSchema.of(Schemas.stringSchema().withKeyword("format", "noFoobar").toJson());

    OutputUnit ouSuccess = Validator.create(dummySchema, options).validate("foobar");
    assertThat(ouSuccess.getValid()).isTrue();

    JsonFormatValidator formatValidator = (instanceType, format, instance) -> {
      if (instanceType == "string" && "noFoobar".equals(format) && "foobar".equalsIgnoreCase(instance.toString())) {
        return "no foobar allowed";
      }
      return null;
    };
    OutputUnit ouFailed = Validator.create(dummySchema, options, formatValidator).validate("foobar");
    assertThat(ouFailed.getValid()).isFalse();
    assertThat(ouFailed.getErrors()).hasSize(1);
    assertThat(ouFailed.getErrors().get(0).getError()).isEqualTo("no foobar allowed");
  }
}
