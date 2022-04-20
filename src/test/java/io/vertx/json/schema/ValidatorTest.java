package io.vertx.json.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.*;
import io.vertx.json.schema.validator.Schema;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidatorTest {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidates() {
    final Validator validator = Validator.create(Schema.fromJson(new JsonObject().put("type", "number")));

    assertThat(validator.validate(7).valid())
      .isEqualTo(true);

    assertThat(validator.validate("hello world").valid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testAddsSchema() {
    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject()
          .put("$id", "https://foo.bar/baz")
          .put("$ref", "/beep")));

    validator.addSchema(Schema.fromJson(
      new JsonObject()
        .put("$id", "https://foo.bar/beep")
        .put("type", "boolean")));

    assertThat(validator.validate(true).valid())
      .isEqualTo(true);
    assertThat(validator.validate("hello world").valid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testAddsSchemaWithSpecifiedId() {
    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject()
          .put("$id", "https://foo.bar/baz")
          .put("$ref", "/beep")));

    validator.addSchema(Schema.fromJson(
        new JsonObject()
          .put("$id", "https://foo.bar/beep")
          .put("type", "boolean")));

    assertThat(validator.validate(true).valid())
      .isEqualTo(true);
    assertThat(validator.validate("hello world").valid())
      .isEqualTo(false);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidateAllArrayEntriesWithNestedErrors() {
    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject()
          .put("type", "array")
          .put("items", new JsonObject()
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("required", new JsonArray().add("name").add("email")))),
      new ValidatorOptions()
        .setDraft(Draft.DRAFT201909)
        .setShortCircuit(false));

    final ValidationResult res = validator.validate(
      new JsonArray()
        .add(new JsonObject().put("name", "hello"))   // missing email
        .add(new JsonObject().put("email", "a@b.c"))  // missing name
    );

    assertThat(res.valid()).isFalse();
    assertThat(res.errors().size()).isEqualTo(4);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testValidateAllObjectPropertiesWithNestedErrors() {
    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject()
          .put("type", "object")
          .put("properties", new JsonObject()
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("number", new JsonObject().put("type", "number"))
            .put("required", new JsonArray().add("name").add("email").add("number")))),
      new ValidatorOptions()
        .setDraft(Draft.DRAFT201909)
        .setShortCircuit(false));

    final ValidationResult res = validator.validate(
      new JsonObject()
        .put("name", "hello")
        .put("email", 5)      // invalid type
        .put("number", "Hello")   // invalid type
    );


    assertThat(res.valid()).isFalse();
    assertThat(res.errors().size()).isEqualTo(4);
  }

  @Test
  public void testRecursive() {

    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject("{\"$ref\":\"http://json-schema.org/draft-04/schema#\"}")),
      new ValidatorOptions()
        .setDraft(Draft.DRAFT4)
        .setShortCircuit(false)
        .setBaseUri("https://github.com/cfworker"));

    validator.addSchema(
      "http://json-schema.org/draft-04/schema",
      Schema.fromJson(new JsonObject("{\n" +
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

    final ValidationResult res = validator.validate(
      new JsonObject("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}"));


    assertThat(res.valid()).isTrue();
  }

  @Test
  public void testQuotedProps() {

    final Validator validator = Validator.create(
      Schema.fromJson(
        new JsonObject("{\"properties\":{\"foo\\\"bar\":{\"$ref\":\"#/definitions/foo%22bar\"}},\"definitions\":{\"foo\\\"bar\":{\"type\":\"number\"}}}")),
      new ValidatorOptions()
        .setDraft(Draft.DRAFT4)
        .setShortCircuit(false)
        .setBaseUri("https://github.com/cfworker"));

    validator.addSchema(
      "http://json-schema.org/draft-04/schema",
      Schema.fromJson(new JsonObject("{\n" +
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

    final ValidationResult res = validator.validate(
      new JsonObject("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}"));


    assertThat(res.valid()).isTrue();
  }
}
