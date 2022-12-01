package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
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

  @Test
  public void testResolveRefsFromRepository(Vertx vertx) {

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT4).setBaseUri("https://vertx.io"));

    try {
      repository.resolve(JsonSchema.of(new JsonObject(vertx.fileSystem().readFileBlocking("resolve/api.json"))));
      // fail
      fail("Should fail as no other references are loaded");
    } catch (SchemaException e) {
      // OK
    }
  }

  @Test
  public void testResolveRefsFromRepositoryWithRefs(Vertx vertx) {

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT4).setBaseUri("https://vertx.io"));

    for (String uri : Arrays.asList("pet.api.json", "pet.model.json", "store.api.json", "store.model.json", "user.api.json", "user.model.json")) {
      repository
        .dereference(uri, JsonSchema.of(new JsonObject(vertx.fileSystem().readFileBlocking("resolve/" + uri))));
    }

    assertThat(repository.resolve(JsonSchema.of(new JsonObject(vertx.fileSystem().readFileBlocking("resolve/api.json")))).encode().length())
      .isEqualTo(137509);
  }

  @Test
  public void testResolveRefsFromRepositoryWithRefsByRef(Vertx vertx) {

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT4).setBaseUri("https://vertx.io"));

    for (String uri : Arrays.asList("api.json", "pet.api.json", "pet.model.json", "store.api.json", "store.model.json", "user.api.json", "user.model.json")) {
      repository
        .dereference(uri, JsonSchema.of(new JsonObject(vertx.fileSystem().readFileBlocking("resolve/" + uri))));
    }

    assertThat(repository.resolve("api.json").encode().length())
      .isEqualTo(137509);
  }

  @Test
  public void testResolveRefsWithinArray(Vertx vertx) {

    JsonSchema schema = JsonSchema.of(new JsonObject(vertx.fileSystem().readFileBlocking("resolve/array.json")));

    JsonObject json = schema.resolve();

    assertThat(json.getJsonArray("parameters").getValue(0))
      .isInstanceOf(JsonObject.class);
  }

  @Test
  public void testResolveShouldHaveNoRefReferences(Vertx vertx) {

    Buffer source = vertx.fileSystem().readFileBlocking("resolve/petstore.json");
    Pattern ref = Pattern.compile("\\$ref", Pattern.MULTILINE);
    assertThat(ref.matcher(source.toString()).find()).isTrue();

    JsonObject json = JsonSchema.of(new JsonObject(source)).resolve();

    assertThat(ref.matcher(json.encode()).find()).isFalse();
  }
}
