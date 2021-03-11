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
}
