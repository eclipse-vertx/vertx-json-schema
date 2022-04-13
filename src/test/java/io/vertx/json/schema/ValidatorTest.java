package io.vertx.json.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.*;
import io.vertx.json.schema.validator.Schema;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
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
          .put("type", "boolean")),
      "https://foo.bar/beep");

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
}
