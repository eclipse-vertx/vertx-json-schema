/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(VertxExtension.class)
public class JavaUtilTest {

  @Test
  public void schemaAndValidationUsingMap(Vertx vertx) {

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(router);

    Schema schema = parser.parse(
      new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("app"))
        .put("properties", new JsonObject()
          .put("app", new JsonObject()
            .put("type", "string")
            .put("minLength", 3))));

    Map<String, String> json = new HashMap<>();
    json.put("app", "abcd");

    // OK
    schema.validateSync(json);

    try {
      json = new HashMap<>();
      json.put("app", "ab");
      // Fail
      schema.validateSync(json);
      Assertions.fail("Should have thrown");
    } catch (ValidationException e) {
      // OK
    }
  }

  @Test
  public void schemaAndValidationUsingList(Vertx vertx) {

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(router);

    Schema schema = parser.parse(
      new JsonObject()
        .put("type", "array")
        .put("items", new JsonObject()
          .put("type", "number")));

    List json = new ArrayList();
    json.add(1);
    json.add(2);
    json.add(3);

    // OK
    schema.validateSync(json);

    try {
      json = new ArrayList<>();
      json.add(1);
      json.add(true);
      json.add(3);

      // Fail
      schema.validateSync(json);
      Assertions.fail("Should have thrown");
    } catch (ValidationException e) {
      // OK
    }
  }

  @Test
  public void nestedComplexExample(Vertx vertx) {

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(router);

    Schema schema = parser.parse(
      new JsonObject(
        "{\n" +
          "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
          "  \"$id\": \"https://example.com/product.schema.json\",\n" +
          "  \"title\": \"Product\",\n" +
          "  \"description\": \"A product from Acme's catalog\",\n" +
          "  \"type\": \"object\",\n" +
          "  \"properties\": {\n" +
          "    \"productId\": {\n" +
          "      \"description\": \"The unique identifier for a product\",\n" +
          "      \"type\": \"integer\"\n" +
          "    },\n" +
          "    \"productName\": {\n" +
          "      \"description\": \"Name of the product\",\n" +
          "      \"type\": \"string\"\n" +
          "    },\n" +
          "    \"price\": {\n" +
          "      \"description\": \"The price of the product\",\n" +
          "      \"type\": \"number\",\n" +
          "      \"exclusiveMinimum\": 0\n" +
          "    },\n" +
          "    \"tags\": {\n" +
          "      \"description\": \"Tags for the product\",\n" +
          "      \"type\": \"array\",\n" +
          "      \"items\": {\n" +
          "        \"type\": \"string\"\n" +
          "      },\n" +
          "      \"minItems\": 1,\n" +
          "      \"uniqueItems\": true\n" +
          "    },\n" +
          "    \"dimensions\": {\n" +
          "      \"type\": \"object\",\n" +
          "      \"properties\": {\n" +
          "        \"length\": {\n" +
          "          \"type\": \"number\"\n" +
          "        },\n" +
          "        \"width\": {\n" +
          "          \"type\": \"number\"\n" +
          "        },\n" +
          "        \"height\": {\n" +
          "          \"type\": \"number\"\n" +
          "        }\n" +
          "      },\n" +
          "      \"required\": [ \"length\", \"width\", \"height\" ]\n" +
          "    }\n" +
          "  },\n" +
          "  \"required\": [ \"productId\", \"productName\", \"price\" ]\n" +
          "}\n"));


    Map json = new HashMap();

    json.put("productId", 1);
    json.put("productName", "Vert.x");
    json.put("price", 9.99);

    List tags = new ArrayList();
    tags.add("awesome");
    tags.add("sauce");

    json.put("tags", tags);

    Map dimensions = new HashMap();
    dimensions.put("length", 42);
    dimensions.put("width", 56);
    dimensions.put("height", 11);

    json.put("dimensions", dimensions);


    // OK
    schema.validateSync(json);
  }
}
