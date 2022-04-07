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
package io.vertx.json.schema.draft202012.dsl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArrayKeyword;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class TupleSchemaBuilder extends SchemaBuilder<TupleSchemaBuilder, ArrayKeyword> {

  // For items keyword as list of schemas
  private final List<SchemaBuilder> prefixItemsList;
  private SchemaBuilder items;

  TupleSchemaBuilder() {
    super(SchemaType.ARRAY);
    this.prefixItemsList = new LinkedList<>();
  }

  @Fluent
  public TupleSchemaBuilder item(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.prefixItemsList.add(schemaBuilder);
    return this;
  }

  @Fluent
  public TupleSchemaBuilder additionalItems(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.items = schemaBuilder;
    return this;
  }

  public List<SchemaBuilder> getPrefixItemsList() {
    return prefixItemsList;
  }

  public SchemaBuilder getItems() {
    return items;
  }

  @Override
  public JsonObject toJson() {
    if (!prefixItemsList.isEmpty())
      this.keywords.put("prefixItems", () -> prefixItemsList.stream().collect(JsonArray::new, (ja, s) -> ja.add(s.toJson()), JsonArray::addAll));
    if (items != null)
      this.keywords.put("items", items::toJson);
    return super.toJson();
  }
}
