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
package io.vertx.json.schema.common.dsl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.common.SchemaURNId;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Entry point for schema dsl. Look at the doc for more info
 *
 * @param <T>
 * @param <K>
 */
public abstract class SchemaBuilder<T extends SchemaBuilder<?, ?>, K extends Keyword> {

  protected SchemaType type;
  protected final Map<String, Supplier<Object>> keywords;
  protected URI id;
  final T self;

  @SuppressWarnings("unchecked")
  public SchemaBuilder(SchemaType type) {
    this.type = type;
    this.keywords = new HashMap<>();
    this.id = new SchemaURNId().toURI();
    this.self = (T) this;
    if (type != null)
      type(type);
  }

  @Fluent
  public T alias(String alias) {
    this.id = new SchemaURNId(alias).toURI();
    return self;
  }

  @Fluent
  public T id(JsonPointer id) {
    this.id = id.toURI();
    return self;
  }

  @Fluent
  public T with(K keyword) {
    this.keywords.put(keyword.getKeyword(), keyword.getValueSupplier());
    return self;
  }

  @Fluent
  public T with(K... keywords) {
    for (Keyword k : keywords) {
      this.keywords.put(k.getKeyword(), k.getValueSupplier());
    }
    return self;
  }

  @Fluent
  public T withKeyword(String key, Object value) {
    this.keywords.put(key, () -> value);
    return self;
  }

  @Fluent
  public T defaultValue(Object defaultValue) {
    keywords.put("default", () -> defaultValue);
    return self;
  }

  @Fluent
  public T fromJson(JsonObject object) {
    object.forEach(e -> keywords.put(e.getKey(), e::getValue));
    return self;
  }

  @Fluent
  public T nullable() {
    keywords.put("type", () -> new JsonArray().add(type.getName()).add("null"));
    return self;
  }

  @Fluent
  public T type(SchemaType type) {
    this.type = type;
    keywords.put("type", type::getName);
    return self;
  }

  public SchemaType getType() {
    return type;
  }

  public JsonObject toJson() {
    JsonObject res = keywords
      .entrySet()
      .stream()
      .collect(JsonObject::new, (jo, e) -> jo.put(e.getKey(), e.getValue().get()), JsonObject::mergeIn);
    res.put("$id", id.toString());
    return res;
  }

  /**
   * @deprecated This method creates an hard link to the parser which we want to avoid
   */
  @Deprecated
  public final Schema build(SchemaParser parser) {
    return parser.parse(toJson(), JsonPointer.fromURI(id));
  }

}
