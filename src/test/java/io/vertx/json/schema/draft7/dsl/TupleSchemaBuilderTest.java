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

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.asserts.MyAssertions;
import org.junit.jupiter.api.Test;

import static io.vertx.core.json.pointer.JsonPointer.create;
import static io.vertx.json.schema.TestUtils.entry;
import static io.vertx.json.schema.common.dsl.Schemas.*;

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

    MyAssertions.assertThat(generated)
      .removingEntry("$id")
      .containsEntry("type", "array");

    MyAssertions.assertThat(generated)
      .extracting(create().append("items").append("0"))
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "number"));

    MyAssertions.assertThat(generated)
      .extracting(create().append("items").append("1"))
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "string"));

    MyAssertions.assertThat(generated)
      .extractingKey("additionalItems")
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "object"));
  }

}
