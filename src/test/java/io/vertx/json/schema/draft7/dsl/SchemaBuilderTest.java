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
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.common.dsl.GenericSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.function.BiFunction;

import static io.vertx.json.schema.TestUtils.entry;
import static io.vertx.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.json.schema.draft7.dsl.Keywords.type;
import static io.vertx.json.schema.draft7.dsl.Schemas.*;

public class SchemaBuilderTest {

  @Test
  public void testNullableSchema() {
    assertThat(
      intSchema().nullable().toJson()
    )
      .removingEntry("$id")
      .extractingKey("type")
      .containsAllAndOnlyItems("integer", "null");
  }

  @Test
  public void testMultipleTypes() {
    assertThat(
      schema()
        .with(
          type(SchemaType.INT, SchemaType.STRING)
        )
        .toJson()
    )
      .removingEntry("$id")
      .extractingKey("type")
      .containsAllAndOnlyItems("integer", "string");
  }

  @Test
  public void testBooleanSchema() {
    assertThat(
      booleanSchema()
        .toJson()
    )
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("type", "boolean"));
  }

  @Test
  public void testFromJson() {
    assertThat(
      schema()
        .fromJson(new JsonObject().put("mykey", "myvalue"))
        .toJson()
    )
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("mykey", "myvalue"));
  }

  @Test
  public void testDefault() {
    assertThat(
      schema()
        .defaultValue("bla")
        .toJson()
    )
      .removingEntry("$id")
      .containsAllAndOnlyEntries(entry("default", "bla"));
  }

  @Test
  public void testId() {
    assertThat(
      schema()
        .id(JsonPointer.fromURI(URI.create("#/bla")))
        .toJson()
    )
      .containsAllAndOnlyEntries(entry("$id", "#/bla"));
  }

  @Test
  public void testGeneratedId() {
    assertThat(
      schema()
        .toJson()
    )
      .extractingKey("$id")
      .asString()
      .startsWith("urn:vertxschemas:");
  }

  @Test
  public void testCustomAlias() {
    assertThat(
      schema()
        .alias("bla")
        .toJson()
    )
      .extractingKey("$id")
      .asString()
      .startsWith("urn:vertxschemas:bla");
  }

  @Test
  public void testConstSchema() {
    assertThat(
      constSchema("hello").toJson()
    )
      .containsEntry("const", "hello");
  }

  @Test
  public void testEnumSchema() {
    assertThat(
      enumSchema("hello", 10, 8.5d).toJson()
    )
      .containsEntry("enum", new JsonArray().add("hello").add(10).add(8.5d));
  }

  @Test
  public void testRefSchema() {
    assertThat(
      ref(JsonPointer.create()).toJson()
    )
      .containsEntry("$ref", JsonPointer.create().toURI().toString());
  }

  @Test
  public void testRefToAlias() {
    assertThat(
      refToAlias("hello").toJson()
    )
      .extractingKey("$ref")
      .asString()
      .startsWith("urn:vertxschemas:hello");
  }

  private void combinatorsTest(String keywordName, BiFunction<SchemaBuilder, SchemaBuilder, GenericSchemaBuilder> combinatorFactory) {
    MyAssertions.assertThat(
      combinatorFactory.apply(numberSchema(), intSchema()).toJson()
    )
      .extractingKey(keywordName)
      .containsItemSatisfying(
        i -> MyAssertions.assertThatJson(i).containsEntry("type", "number")
      )
      .containsItemSatisfying(
        i -> MyAssertions.assertThatJson(i).containsEntry("type", "integer")
      );
  }

  @Test
  public void testAllOf() {
    combinatorsTest("allOf", (s1, s2) -> allOf(s1, s2));
  }

  @Test
  public void testAnyOf() {
    combinatorsTest("anyOf", (s1, s2) -> anyOf(s1, s2));
  }

  @Test
  public void testOneOf() {
    combinatorsTest("oneOf", (s1, s2) -> oneOf(s1, s2));
  }

  @Test
  public void testNot() {
    assertThat(
      not(numberSchema()).toJson()
    )
      .extractingKey("not")
      .containsEntry("type", "number");
  }

  @Test
  public void testIfThenElse() {
    assertThat(
      ifThenElse(numberSchema(), intSchema(), stringSchema())
        .toJson()
    )
      .containsEntrySatisfying("if", v -> MyAssertions.assertThatJson(v).containsEntry("type", "number"))
      .containsEntrySatisfying("then", v -> MyAssertions.assertThatJson(v).containsEntry("type", "integer"))
      .containsEntrySatisfying("else", v -> MyAssertions.assertThatJson(v).containsEntry("type", "string"));
  }

}
