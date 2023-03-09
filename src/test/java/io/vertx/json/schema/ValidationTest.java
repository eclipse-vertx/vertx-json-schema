package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ValidationTest {

  private final static JsonSchemaOptions SCHEMA_OPTIONS = new JsonSchemaOptions().setDraft(Draft.DRAFT202012).setBaseUri("app://");

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
  }
}
