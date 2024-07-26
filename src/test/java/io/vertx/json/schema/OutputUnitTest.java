package io.vertx.json.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputUnitTest {

  @Test
  public void testFlagModeInvalid() {
    OutputUnit outputUnit = new OutputUnit(new JsonObject().put("valid", false));
    try {
      outputUnit.checkValidity();
      fail("Should not reach here");
    } catch (JsonSchemaValidationException e) {
      assertEquals("JsonSchema Validation error", e.getMessage());
      assertNull(e.location());
    }
  }
  @Test
  public void testFlagModeValid() {
    OutputUnit outputUnit = new OutputUnit(new JsonObject().put("valid", true));
    try {
      outputUnit.checkValidity();
    } catch (JsonSchemaValidationException e) {
      fail("Should not reach here");
    }
  }

  @Test
  public void testBasicMode() {
    OutputUnit outputUnit = new OutputUnit(new JsonObject(
      "{\n" +
        "  \"valid\": false,\n" +
        "  \"errors\": [\n" +
        "    {\n" +
        "      \"keywordLocation\": \"\",\n" +
        "      \"instanceLocation\": \"\",\n" +
        "      \"error\": \"A subschema had errors.\",\n" +
        "      \"errorType\": \"INVALID_VALUE\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"keywordLocation\": \"/items/$ref\",\n" +
        "      \"absoluteKeywordLocation\":\n" +
        "        \"https://example.com/polygon#/$defs/point\",\n" +
        "      \"instanceLocation\": \"/1\",\n" +
        "      \"error\": \"A subschema had errors.\",\n" +
        "      \"errorType\": \"INVALID_VALUE\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"keywordLocation\": \"/items/$ref/required\",\n" +
        "      \"absoluteKeywordLocation\":\n" +
        "        \"https://example.com/polygon#/$defs/point/required\",\n" +
        "      \"instanceLocation\": \"/1\",\n" +
        "      \"error\": \"Required property 'y' not found.\",\n" +
        "      \"errorType\": \"MISSING_VALUE\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"keywordLocation\": \"/items/$ref/additionalProperties\",\n" +
        "      \"absoluteKeywordLocation\":\n" +
        "        \"https://example.com/polygon#/$defs/point/additionalProperties\",\n" +
        "      \"instanceLocation\": \"/1/z\",\n" +
        "      \"error\": \"Additional property 'z' found but was invalid.\",\n" +
        "      \"errorType\": \"INVALID_VALUE\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"keywordLocation\": \"/minItems\",\n" +
        "      \"instanceLocation\": \"\",\n" +
        "      \"error\": \"Expected at least 3 items but found 2\",\n" +
        "      \"errorType\": \"MISSING_VALUE\"\n" +
        "    }\n" +
        "  ],\n" +
        "\"errorType\": \"MISSING_VALUE\"\n" +
        "}"
    ));
    try {
      outputUnit.checkValidity();
      fail("Should not reach here");
    } catch (JsonSchemaValidationException e) {
      assertEquals("Expected at least 3 items but found 2", e.getMessage());
      assertNull(e.location());
      assertEquals(OutputErrorType.MISSING_VALUE, e.errorType());
    }
  }

  @Test
  public void testSpecFlag() {
    JsonSchema schema = JsonSchema.of(new JsonObject(
      "{\n" +
        "  \"$id\": \"https://example.com/polygon\",\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "  \"$defs\": {\n" +
        "    \"point\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"properties\": {\n" +
        "        \"x\": { \"type\": \"number\" },\n" +
        "        \"y\": { \"type\": \"number\" }\n" +
        "      },\n" +
        "      \"additionalProperties\": false,\n" +
        "      \"required\": [ \"x\", \"y\" ]\n" +
        "    }\n" +
        "  },\n" +
        "  \"type\": \"array\",\n" +
        "  \"items\": { \"$ref\": \"#/$defs/point\" },\n" +
        "  \"minItems\": 3\n" +
        "}"));

    Validator validator = Validator.create(
      schema,
      new JsonSchemaOptions()
        .setDraft(Draft.DRAFT202012)
        .setBaseUri("urn:")
        .setOutputFormat(OutputFormat.Flag));

    JsonArray input = new JsonArray(
      "[\n" +
        "  {\n" +
        "    \"x\": 2.5,\n" +
        "    \"y\": 1.3\n" +
        "  },\n" +
        "  {\n" +
        "    \"x\": 1,\n" +
        "    \"z\": 6.7\n" +
        "  }\n" +
        "]");

    OutputUnit result = validator.validate(input);

    assertFalse(result.getValid());
    assertNull(result.getErrors());
  }

  @Test
  public void testSpecBasic() {
    JsonSchema schema = JsonSchema.of(new JsonObject(
      "{\n" +
        "  \"$id\": \"https://example.com/polygon\",\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "  \"$defs\": {\n" +
        "    \"point\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"properties\": {\n" +
        "        \"x\": { \"type\": \"number\" },\n" +
        "        \"y\": { \"type\": \"number\" }\n" +
        "      },\n" +
        "      \"additionalProperties\": false,\n" +
        "      \"required\": [ \"x\", \"y\" ]\n" +
        "    }\n" +
        "  },\n" +
        "  \"type\": \"array\",\n" +
        "  \"items\": { \"$ref\": \"#/$defs/point\" },\n" +
        "  \"minItems\": 3\n" +
        "}"));

    Validator validator = Validator.create(
      schema,
      new JsonSchemaOptions()
        .setDraft(Draft.DRAFT202012)
        .setBaseUri("urn:")
        .setOutputFormat(OutputFormat.Basic));

    JsonArray input = new JsonArray(
      "[\n" +
        "  {\n" +
        "    \"x\": 2.5,\n" +
        "    \"y\": 1.3\n" +
        "  },\n" +
        "  {\n" +
        "    \"x\": 1,\n" +
        "    \"z\": 6.7\n" +
        "  }\n" +
        "]");

    OutputUnit result = validator.validate(input);

    assertFalse(result.getValid());
    try {
      result.checkValidity();
    } catch (JsonSchemaValidationException e) {
      assertNotNull(e.location());
    }
    assertEquals(5, result.getErrors().size());
  }
}
