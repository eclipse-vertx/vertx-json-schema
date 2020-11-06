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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.asserts.MyAssertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.vertx.core.json.pointer.JsonPointer.create;
import static io.vertx.json.schema.TestUtils.entry;
import static io.vertx.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.json.schema.draft7.dsl.Schemas.*;

public class ObjectSchemaBuilderTest {

  @Test
  public void testProperties() {
    JsonObject generated = objectSchema()
      .optionalProperty("optionalProp", numberSchema())
      .requiredProperty("requiredProp", stringSchema())
      .patternProperty(Pattern.compile("[0-9]{3,5}"), intSchema())
      .additionalProperties(arraySchema())
      .toJson();

    MyAssertions.assertThat(generated)
      .removingEntry("$id")
      .containsEntry("type", "object")
      .containsEntry("required", new JsonArray().add("requiredProp"));

    MyAssertions.assertThat(generated)
      .extracting(create().append("properties").append("optionalProp"))
      .containsEntry("type", "number");

    MyAssertions.assertThat(generated)
      .extracting(create().append("properties").append("requiredProp"))
      .containsEntry("type", "string");

    MyAssertions.assertThat(generated)
      .extracting(create().append("patternProperties").append(Pattern.compile("[0-9]{3,5}").toString()))
      .containsEntry("type", "integer");

    MyAssertions.assertThat(generated)
      .extractingKey("additionalProperties")
      .containsEntry("type", "array");
  }

  @Test
  public void testKeywords() {
    JsonObject generated = objectSchema()
      .with(maxProperties(10), minProperties(1), propertyNames(stringSchema()))
      .toJson();

    MyAssertions.assertThat(generated)
      .removingEntry("$id")
      .removingEntry("propertyNames")
      .containsAllAndOnlyEntries(entry("type", "object"), entry("minProperties", 1), entry("maxProperties", 10));

    MyAssertions.assertThat(generated)
      .extractingKey("propertyNames")
      .containsEntry("type", "string");
  }
}
