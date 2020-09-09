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

import static io.vertx.core.json.pointer.JsonPointer.create;
import static io.vertx.ext.json.schema.TestUtils.entry;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.*;

public class TupleSchemaBuilderTest {

  @Test
  public void testItemByItem() {
    JsonObject generated = tupleSchema()
      .item(
        numberSchema()
      ).item(
        stringSchema()
      ).additionalItems(
        objectSchema()
      )
      .toJson();

    assertThat(generated)
      .removingEntry("$id")
      .containsEntry("type", "array");

    assertThat(generated)
      .extracting(create().append("items").append("0"))
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "number"));

    assertThat(generated)
      .extracting(create().append("items").append("1"))
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "string"));

    assertThat(generated)
      .extractingKey("additionalItems")
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "object"));
  }

}
