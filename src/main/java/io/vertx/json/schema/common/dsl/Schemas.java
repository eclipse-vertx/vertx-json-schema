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

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.common.SchemaURNId;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;

@VertxGen
public interface Schemas {

  /**
   * Creates a generic untyped schema. You can add the type keyword using {@link Keywords#type(SchemaType...)}
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder schema() {
    return new GenericSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static NumberSchemaBuilder intSchema() {
    return new NumberSchemaBuilder().asInteger();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static NumberSchemaBuilder numberSchema() {
    return new NumberSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static StringSchemaBuilder stringSchema() {
    return new StringSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static BooleanSchemaBuilder booleanSchema() {
    return new BooleanSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static TupleSchemaBuilder tupleSchema() {
    return new TupleSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ArraySchemaBuilder arraySchema() {
    return new ArraySchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ObjectSchemaBuilder objectSchema() {
    return new ObjectSchemaBuilder();
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder constSchema(@Nullable Object constValue) {
    return new GenericSchemaBuilder().with(new Keyword("const", constValue));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder enumSchema(@Nullable Object... enumValues) {
    return new GenericSchemaBuilder().with(new Keyword("enum", Arrays.asList(enumValues)));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder ref(JsonPointer pointer) {
    Objects.requireNonNull(pointer);
    return new GenericSchemaBuilder().with(new Keyword("$ref", pointer.toURI().toString()));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder refToAlias(String alias) {
    Objects.requireNonNull(alias);
    return ref(new SchemaURNId(alias).toPointer());
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder allOf(SchemaBuilder... allOf) {
    Objects.requireNonNull(allOf);
    return new GenericSchemaBuilder().with(new Keyword("allOf",
      () -> Arrays
        .stream(allOf)
        .collect(Collector.of(JsonArray::new, (j, b) -> j.add(b.toJson()), JsonArray::addAll))
    ));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public static GenericSchemaBuilder anyOf(SchemaBuilder... anyOf) {
    Objects.requireNonNull(anyOf);
    return new GenericSchemaBuilder().with(new Keyword("anyOf",
      () -> Arrays
        .stream(anyOf)
        .collect(Collector.of(JsonArray::new, (j, b) -> j.add(b.toJson()), JsonArray::addAll))
    ));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder oneOf(SchemaBuilder... oneOf) {
    Objects.requireNonNull(oneOf);
    return new GenericSchemaBuilder().with(new Keyword("oneOf",
      () -> Arrays
        .stream(oneOf)
        .collect(Collector.of(JsonArray::new, (j, b) -> j.add(b.toJson()), JsonArray::addAll))
    ));
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder not(SchemaBuilder not) {
    Objects.requireNonNull(not);
    return new GenericSchemaBuilder().with(new Keyword("not", not::toJson));
  }
}
