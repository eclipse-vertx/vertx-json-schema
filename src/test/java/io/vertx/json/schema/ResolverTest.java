package io.vertx.json.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ResolverTest {

  @Test
  public void testResolves() {

    SchemaRepository repo = SchemaRepository.create(new JsonSchemaOptions().setBaseUri("http://vertx.io"));

    JsonSchema schema = JsonSchema
      .of(new JsonObject("{\"$id\":\"http://www.example.com/\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"street_address\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"},\"state\":{\"type\":\"string\"},\"subAddress\":{\"$ref\":\"http://www.example.com/#/definitions/address\"}}},\"req\":{\"required\":[\"billing_address\"]}},\"type\":\"object\",\"properties\":{\"billing_address\":{\"$ref\":\"#/definitions/address\"},\"shipping_address\":{\"$ref\":\"#/definitions/address\"}},\"$ref\":\"#/definitions/req\"}"));

    repo.dereference(schema);

    String before = schema.toString();

    JsonObject res = schema.resolve();

    JsonObject ptr = res.getJsonObject("properties").getJsonObject("billing_address").getJsonObject("properties");
    assertThat(ptr.getJsonObject("city").getString("type"))
      .isEqualTo("string");

    // circular check
    JsonObject circular = ptr.getJsonObject("subAddress").getJsonObject("properties");
    assertThat(circular.getJsonObject("city").getString("type"))
      .isEqualTo("string");

    // array checks
    assertThat(res.getJsonArray("required").getString(0))
      .isEqualTo("billing_address");

    assertThat(res.encodePrettily().contains("__absolute_uri")).isFalse();
    assertThat(res.encodePrettily().contains("__absolute_ref")).isFalse();
    assertThat(res.encodePrettily().contains("__absolute_recursive_ref")).isFalse();

    String after = schema.toString();

    // ensure that the clean up operation doesn't affect the source object
    assertThat(before).isEqualTo(after);

  }

  @Test
  public void testRefResolverFail() throws IOException {
    try {
      JsonObject res =
        JsonSchema
          .of(new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "ref_test", "person_draft201909.json")))))
          .resolve();

      fail("Should not reach here");
    } catch (SchemaException e) {
      // OK
    }
  }
}
