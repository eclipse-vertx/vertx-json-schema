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
package io.vertx.json.schema.draft7.dsl;

import org.junit.jupiter.api.Test;

import static io.vertx.json.schema.TestUtils.entry;
import static io.vertx.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.json.schema.draft7.dsl.Schemas.numberSchema;

public class NumberSchemaBuilderTest {

  @Test
  public void testNumberSchema() {
    assertThat(
      numberSchema().toJson()
    ).removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "number"));
  }

  @Test
  public void testIntegerSchema() {
    assertThat(
      numberSchema().asInteger().toJson()
    ).removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "integer"));
  }

  @Test
  public void testKeywords() {
    assertThat(
      numberSchema()
        .with(multipleOf(2d), exclusiveMaximum(10d), maximum(10d), exclusiveMinimum(10d), minimum(10d))
        .toJson()
    ).removingEntry("$id")
      .containsAllAndOnlyEntries(
        entry("type", "number"),
        entry("multipleOf", 2d),
        entry("exclusiveMaximum", 10d),
        entry("exclusiveMinimum", 10d),
        entry("maximum", 10d),
        entry("minimum", 10d)
      );
  }

}
