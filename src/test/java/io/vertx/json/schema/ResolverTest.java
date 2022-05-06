package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ResolverTest {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testResolves() {
    JsonObject res =
      JsonSchema
        .of(new JsonObject("{\"$id\":\"http://www.example.com/\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"street_address\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"},\"state\":{\"type\":\"string\"},\"subAddress\":{\"$ref\":\"http://www.example.com/#/definitions/address\"}}},\"req\":{\"required\":[\"billing_address\"]}},\"type\":\"object\",\"properties\":{\"billing_address\":{\"$ref\":\"#/definitions/address\"},\"shipping_address\":{\"$ref\":\"#/definitions/address\"}},\"$ref\":\"#/definitions/req\"}"))
        .resolve();

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
  }
}
