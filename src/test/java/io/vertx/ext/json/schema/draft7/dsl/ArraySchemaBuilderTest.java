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
package io.vertx.ext.json.schema.draft7.dsl;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.vertx.ext.json.schema.TestUtils.entry;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.arraySchema;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.numberSchema;

public class ArraySchemaBuilderTest {

  @Test
  public void testItems() {
    JsonObject generated = arraySchema()
      .items(
        numberSchema()
      )
      .toJson();

    assertThat(generated)
      .removingEntry("$id")
      .containsEntry("type", "array")
      .extractingKey("items")
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "number"));
  }

  @Test
  public void testKeywords() {
    JsonObject generated = arraySchema()
      .with(maxItems(10), minItems(1), uniqueItems(), contains(numberSchema()))
      .toJson();

    assertThat(generated)
      .removingEntry("$id")
      .removingEntry("contains")
      .containsAllAndOnlyEntries(
        entry("type", "array"),
        entry("maxItems", 10),
        entry("minItems", 1),
        entry("uniqueItems", true)
      );

    assertThat(generated)
      .extractingKey("contains")
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "number"));
  }

}
