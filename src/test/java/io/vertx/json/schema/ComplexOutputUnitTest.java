package io.vertx.json.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ComplexOutputUnitTest {

  @Test
  public void testSpecFlag() throws IOException {
    JsonSchemaOptions schemaOpts = new JsonSchemaOptions()
      .setDraft(Draft.DRAFT202012)
      .setBaseUri("urn://")
      .setOutputFormat(OutputFormat.Basic);
    SchemaRepository repo = SchemaRepository.create(schemaOpts);

    JsonObject schemaJson = readjson("list_with_elements.json").toJsonObject();
    repo.dereference(JsonSchema.of(schemaJson));

    JsonArray input = readjson("invalid_input.json").toJsonArray();

    Validator validator = repo.validator("#/components/schemas/List");

    OutputUnit result = validator.validate(input);

    assertFalse(result.getValid());
    result.getErrors().forEach(ou -> {
      System.out.println(ou.getKeywordLocation() + ": " + ou.getError());
    });
  }

  private static Buffer readjson(String fileName) throws IOException {
    return Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources",
      "outputunit", fileName)));
  }
}
