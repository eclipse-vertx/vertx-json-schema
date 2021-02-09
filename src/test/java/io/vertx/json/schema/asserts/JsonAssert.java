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
package io.vertx.json.schema.asserts;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.assertj.core.api.AbstractAssert;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonAssert extends AbstractAssert<JsonAssert, Object> {
  public JsonAssert(Object actual) {
    super(actual, JsonAssert.class);
  }

  public JsonAssert extracting(JsonPointer pointer) {
    return new JsonAssert(pointer.queryJson(actual));
  }

  public JsonAssert extractingKey(String keyName) {
    isJsonObject();
    return new JsonAssert(((JsonObject) actual).getValue(keyName));
  }

  public JsonAssert removingEntry(String keyName) {
    containsKey(keyName);

    JsonObject jo = ((JsonObject) actual).copy();
    jo.remove(keyName);

    return new JsonAssert(jo);
  }

  public JsonAssert isJsonObject() {
    assertThat(actual).isInstanceOf(JsonObject.class);
    return this;
  }

  public JsonAssert isJsonArray() {
    assertThat(actual).isInstanceOf(JsonArray.class);
    return this;
  }

  public JsonAssert containsEntry(String keyword, Object value) {
    isJsonObject();

    JsonObject jo = (JsonObject) actual;
    assertThat(jo.getValue(keyword)).isEqualTo(value);

    return this;
  }

  public JsonAssert containsEntrySatisfying(String keyword, Consumer<Object> requirement) {
    isJsonObject();

    JsonObject jo = (JsonObject) actual;
    assertThat(jo.getValue(keyword)).satisfies(requirement);

    return this;
  }

  public JsonAssert containsKey(String keyword) {
    isJsonObject();

    JsonObject jo = (JsonObject) actual;
    assertThat(jo.containsKey(keyword)).isTrue();

    return this;
  }

  public JsonAssert containsItem(Object value) {
    isJsonArray();

    JsonArray ja = (JsonArray) actual;

    assertThat(ja.contains(value)).isTrue();

    return this;

  }

  public JsonAssert containsItemSatisfying(Consumer<Object> requirement) {
    isJsonArray();

    JsonArray ja = (JsonArray) actual;

    boolean found = false;
    for (Object obj : ja) {
      try {
        requirement.accept(obj);
        found = true;
      } catch (AssertionError a) {
      }
    }

    if (!found) failWithMessage("Cannot find an element in the array satisfying the requirement");

    return this;

  }

  @SuppressWarnings("unchecked")
  public JsonAssert containsAllAndOnlyEntries(Map.Entry... entries) {
    isJsonObject();

    JsonObject jo = (JsonObject) actual;
    assertThat(jo.size()).isEqualTo(entries.length);
    for (Map.Entry<String, Object> e : entries) {
      assertThat(jo.getValue(e.getKey())).isEqualTo(e.getValue());
    }

    return this;
  }

  public JsonAssert containsAllAndOnlyItems(Object... items) {
    isJsonArray();

    JsonArray ja = (JsonArray) actual;
    assertThat(ja.size()).isEqualTo(items.length);
    for (Object i : items) {
      assertThat(ja.contains(i)).isTrue();
    }

    return this;
  }

}
