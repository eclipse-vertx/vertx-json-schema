package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class ResolverTest {

  @Test
  public void testResolveCircularRefs() {

    JsonObject schema = new JsonObject("{\"$id\":\"http://www.example.com/\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"street_address\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"},\"state\":{\"type\":\"string\"},\"subAddress\":{\"$ref\":\"http://www.example.com/#/definitions/address\"}}},\"req\":{\"required\":[\"billing_address\"]}},\"type\":\"object\",\"properties\":{\"billing_address\":{\"$ref\":\"#/definitions/address\"},\"shipping_address\":{\"$ref\":\"#/definitions/address\"}},\"$ref\":\"#/definitions/req\"}");

    JsonObject res = Ref.resolve(schema);
    // this is null because the top level ref implies a full object replacement
    assertThat(res.getJsonObject("properties")).isNull();
  }

  @Test
  public void testResolveCircularRefs2() {

    SchemaRepository repo = SchemaRepository.create(new JsonSchemaOptions().setBaseUri("http://vertx.io"));

    JsonObject schema =
      new JsonObject("{\"$id\":\"http://www.example.com/\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"street_address\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"},\"state\":{\"type\":\"string\"},\"subAddress\":{\"$ref\":\"http://www.example.com/#/definitions/address\"}}},\"req\":{\"required\":[\"billing_address\"]}},\"type\":\"object\",\"properties\":{\"billing_address\":{\"$ref\":\"#/definitions/address\"},\"shipping_address\":{\"$ref\":\"#/definitions/address\"}}}");

    JsonObject res = Ref.resolve(schema);

    assertThat(JsonPointer.from("/properties/billing_address/properties/city/type").queryJson(res)).isEqualTo("string");

    // circular check
    assertThat(JsonPointer.from("/properties/billing_address/properties/subAddress/$ref").queryJson(res)).isNull();
    assertThat(JsonPointer.from("/properties/billing_address/properties/subAddress/type").queryJson(res)).isEqualTo("object");
  }

  @Test
  public void testRefResolverFail() throws IOException {
    try {
      JsonObject res =
        Ref.resolve(
          new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "ref_test", "person_draft201909.json")))));

      fail("Should not reach here");
    } catch (UnsupportedOperationException e) {
      // OK
    }
  }

  @Test
  public void testResolveRefsWithinArray(Vertx vertx) {

    JsonObject schema = new JsonObject(vertx.fileSystem().readFileBlocking("resolve/array.json"));
    JsonObject json = Ref.resolve(schema);

    assertThat(json.getJsonArray("parameters").getValue(0))
      .isInstanceOf(JsonObject.class);
  }

  @Test
  public void testResolveShouldHaveNoRefReferences(Vertx vertx) {

    Buffer source = vertx.fileSystem().readFileBlocking("resolve/petstore.json");
    Pattern ref = Pattern.compile("\\$ref", Pattern.MULTILINE);
    assertThat(ref.matcher(source.toString()).find()).isTrue();

    JsonObject json = Ref.resolve(new JsonObject(source));

    assertThat(ref.matcher(json.encode()).find()).isFalse();
  }

  @Test
  public void testResolveCircularRefsDoWork(Vertx vertx) {

    SchemaRepository repo = SchemaRepository.create(new JsonSchemaOptions().setBaseUri("http://vertx.io"));

    JsonObject document = new JsonObject("{\"$id\":\"http://www.example.com/\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"street_address\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"},\"state\":{\"type\":\"string\"},\"subAddress\":{\"$ref\":\"http://www.example.com/#/definitions/address\"}}},\"req\":{\"required\":[\"billing_address\"]}},\"type\":\"object\",\"properties\":{\"billing_address\":{\"$ref\":\"#/definitions/address\"},\"shipping_address\":{\"$ref\":\"#/definitions/address\"}}}");

    JsonSchema schema = JsonSchema.of(document);

    repo.dereference(schema);

    JsonObject res = Ref.resolve(document);

    // wrap it in a schema
    JsonSchema schema2 = JsonSchema.of(res);

    // simple check
    JsonObject fixture = new JsonObject(vertx.fileSystem().readFileBlocking("resolve/circular.json"));

    // Canary
    OutputUnit result = repo.validator(schema).validate(fixture);
    assertThat(result.getValid()).isTrue();

    // Real test (given that the resolved holds the dereferenced metadata, it works as it picks the dereferneced schema
    // from the __absolute_uri__ field
    OutputUnit result2 = repo.validator(schema2).validate(fixture);
    assertThat(result2.getValid()).isTrue();
  }
}
