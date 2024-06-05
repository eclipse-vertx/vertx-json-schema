package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
class ValidationTest {

  private final static JsonSchemaOptions SCHEMA_OPTIONS = new JsonSchemaOptions()
      .setDraft(Draft.DRAFT202012).setBaseUri("app://").setOutputFormat(OutputFormat.Basic);

  @Test
  public void testValidate202012RelyingOnDynamicAnchorDynamicRefShouldFail(Vertx vertx) {
    SchemaRepository repository = SchemaRepository.create(SCHEMA_OPTIONS);

    OutputUnit ou = repository
      .preloadMetaSchema(vertx.fileSystem())
      .validator("https://json-schema.org/draft/2020-12/schema")
      .validate(new JsonObject("{\n" +
        "  \"type\" : \"object\",\n" +
        "  \"required\" : [ \"guest\" ],\n" +
        "  \"properties\" : {\n" +
        "    \"guest\" : {\n" +
        "      \"type\" : {\n" +
        // This is not correct
        "        \"type\" : \"string\"\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}"));

    assertThat(ou.getValid()).isFalse();
    assertThat(ou.getErrorType()).isEqualByComparingTo(OutputErrorType.INVALID_VALUE);
  }

  @Test
  public void testValidate202012RelyingOnDynamicAnchorDynamicRefShouldPass(Vertx vertx) {
    SchemaRepository repository = SchemaRepository.create(SCHEMA_OPTIONS);

    OutputUnit ou = repository
      .preloadMetaSchema(vertx.fileSystem())
      .validator("https://json-schema.org/draft/2020-12/schema")
      .validate(new JsonObject("{\n" +
        "  \"type\" : \"object\",\n" +
        "  \"required\" : [ \"guest\" ],\n" +
        "  \"properties\" : {\n" +
        "    \"guest\" : {\n" +
        "      \"type\" : \"string\"\n" +
        "    }\n" +
        "  }\n" +
        "}"));

    assertThat(ou.getValid()).isTrue();
    assertThat(ou.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void testValidHostWithNumbers() {
    final Validator validator = Validator.create(
      JsonSchema.of(new JsonObject("{\"type\":\"object\",\"properties\":{\"host\":{\"type\":\"string\",\"format\":\"hostname\"}}}")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT7)
        .setOutputFormat(OutputFormat.Basic));

    final OutputUnit res = validator.validate(new JsonObject("{\"host\":\"www.3gppnetwork.org\"}"));

    assertThat(res.getValid()).isTrue();
    assertThat(res.getErrors()).isNull();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void testComplexSchema() throws JsonSchemaValidationException {
    final Validator validator = Validator.create(
      JsonSchema.of(new JsonObject("{ \"description\": \"TS29510_Nnrf_NFManagement.yaml\", \"javaType\": \"ts3gpp.ChfInfo\", \"type\": \"object\", \"properties\": { \"supiRangeList\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"supi-range.json\" } }, \"gpsiRangeList\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"identity-range.json\" } }, \"plmnRangeList\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"plmn-range.json\" } }, \"groupId\": { \"type\": \"string\" }, \"primaryChfInstance\": { \"type\": \"string\", \"pattern\": \"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$\" }, \"secondaryChfInstance\": { \"type\": \"string\", \"pattern\": \"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$\" } }, \"required\": [ \"primaryChfInstance\", \"secondaryChfInstance\" ] }")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT7)
        .setOutputFormat(OutputFormat.Basic));

    final OutputUnit res = validator.validate(new JsonObject("{\"additionalProperties\":{\"supiRangeList\":[{\"start\":\"111\",\"end\":\"999\",\"pattern\":\"pattern\"}],\"gpsiRangeList\":[{\"start\":\"123\",\"end\":\"789\",\"pattern\":\"pattern-value\"}],\"plmnRangeList\":[{\"start\":\"11122\",\"end\":\"11133\",\"pattern\":\"pattern-value\"}],\"groupId\":\"123456789\",\"primaryChfInstance\":\"6f134298-939a-47d6-9566-fd0030517ac2\",\"secondaryChfInstance\":\"66ac6eea-db7c-44e4-a8e8-2a5d6e5184ef\"}}"));

    try {
      res.checkValidity();
      fail("Should have thrown an exception");
    } catch (JsonSchemaValidationException e) {
      assertThat(e.errorType()).isEqualByComparingTo(res.getErrorType());
      assertThat(e.errorType()).isEqualByComparingTo(OutputErrorType.MISSING_VALUE);
    }
  }

  @Test
  public void testComplexSchema2() throws JsonSchemaValidationException {
    final Validator validator = Validator.create(
      JsonSchema.of(new JsonObject("{ \"description\": \"TS29510_Nnrf_NFManagement.yaml\", \"javaType\": \"ts3gpp.LmfInfo\", \"type\": \"object\", \"properties\": { \"externalClientType\": { \"type\": \"array\", \"default\": null, \"items\": { \"$ref\": \"enums/external-client-type.json\", \"existingJavaType\": \"ts3gpp.enums.ExternalClientType\" } }, \"lmfId\": { \"type\": \"string\" }, \"servingAccessTypes\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"enums/access-type.json\", \"existingJavaType\": \"ts3gpp.enums.AccessType\" } }, \"servingAnNodeTypes\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"enums/an-node-type.json\", \"existingJavaType\": \"ts3gpp.enums.AnNodeType\" } }, \"servingRatTypes\": { \"type\": \"array\", \"default\": null, \"minItems\": 1, \"items\": { \"$ref\": \"enums/rat-type.json\", \"existingJavaType\": \"ts3gpp.enums.RatType\" } } } }")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT7)
        .setOutputFormat(OutputFormat.Basic));

    final OutputUnit res = validator.validate(new JsonObject("{\"additionalProperties\":{\"supiRangeList\":[{\"start\":\"111\",\"end\":\"999\",\"pattern\":\"pattern\"}],\"gpsiRangeList\":[{\"start\":\"123\",\"end\":\"789\",\"pattern\":\"pattern-value\"}],\"plmnRangeList\":[{\"start\":\"11122\",\"end\":\"11133\",\"pattern\":\"pattern-value\"}],\"groupId\":\"123456789\",\"primaryChfInstance\":\"6f134298-939a-47d6-9566-fd0030517ac2\",\"secondaryChfInstance\":\"66ac6eea-db7c-44e4-a8e8-2a5d6e5184ef\"}}"));

    // Should be fine!
    res.checkValidity();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void testIssue49() throws JsonSchemaValidationException {

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT201909).setBaseUri("app://"));

    // child schema
    repository.dereference("schema-child.json", JsonSchema.of(
      new JsonObject("{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
        "  \"description\": \"The child schema.\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"additionalProperties\": false,\n" +
        "  \"required\": [\n" +
        "    \"prop1\",\n" +
        "    \"prop2\"\n" +
        "  ],\n" +
        "  \"properties\": {\n" +
        "    \"prop1\": {\n" +
        "      \"description\": \"prop 1\",\n" +
        "      \"type\": \"string\"\n" +
        "    },\n" +
        "    \"prop2\": {\n" +
        "      \"description\": \"prop 2\",\n" +
        "      \"type\": \"integer\"\n" +
        "    }\n" +
        "  }\n" +
        "}")
    ));

    // parent
    repository.dereference("schema-parent.json", JsonSchema.of(
      new JsonObject("{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
        "  \"description\": \"The parent schema.\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"additionalProperties\": false,\n" +
        "  \"required\": [\n" +
        "    \"prop1\",\n" +
        "    \"prop2\"\n" +
        "  ],\n" +
        "  \"properties\": {\n" +
        "    \"prop1\": {\n" +
        "      \"$ref\": \"schema-child.json#/properties/prop1\"\n" +
        "    },\n" +
        "    \"prop2\": {\n" +
        "      \"$ref\": \"schema-child.json#/properties/prop2\"\n" +
        "    }\n" +
        "  }\n" +
        "}")
    ));

    JsonObject json = new JsonObject()
      .put("prop1", "123e4567-e89b-42d3-a456-556642440000")
      .put("prop2", 42);

    OutputUnit res = repository.validator("schema-parent.json").validate(json);
    res.checkValidity();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);

  }

  @Test
  public void testIssue49b() throws JsonSchemaValidationException {

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT201909).setBaseUri("app://"));

    // child schema
    repository.dereference("schema-child.json", JsonSchema.of(
      new JsonObject("{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
        "  \"description\": \"The child schema.\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"additionalProperties\": false,\n" +
        "  \"required\": [\n" +
        "    \"prop1\",\n" +
        "    \"prop2\"\n" +
        "  ],\n" +
        "  \"properties\": {\n" +
        "    \"prop1\": {\n" +
        "      \"description\": \"prop 1\",\n" +
        "      \"type\": \"string\"\n" +
        "    },\n" +
        "    \"prop2\": {\n" +
        "      \"description\": \"prop 2\",\n" +
        "      \"type\": \"integer\"\n" +
        "    }\n" +
        "  }\n" +
        "}")
    ));

    // parent
    repository.dereference("schema-parent.json", JsonSchema.of(
      new JsonObject("{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
        "  \"description\": \"The parent schema.\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"additionalProperties\": false,\n" +
        "  \"required\": [\n" +
        "    \"prop1\",\n" +
        "    \"prop2\"\n" +
        "  ],\n" +
        "  \"properties\": {\n" +
        "    \"prop1\": {\n" +
        "      \"$ref\": \"schema-child.json#/properties/prop1\"\n" +
        "    },\n" +
        "    \"prop2\": {\n" +
        "      \"description\": \"prop 2\",\n" +
        "      \"type\": \"integer\"\n" +
        "    }\n" +
        "  }\n" +
        "}")
    ));

    JsonObject json = new JsonObject()
      .put("prop1", "123e4567-e89b-42d3-a456-556642440000")
      .put("prop2", 42);


    OutputUnit res = repository.validator("schema-parent.json").validate(json);
    res.checkValidity();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }
}
