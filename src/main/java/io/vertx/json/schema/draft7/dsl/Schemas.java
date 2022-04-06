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

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.common.dsl.GenericSchemaBuilder;
import io.vertx.json.schema.common.dsl.Keyword;
import io.vertx.json.schema.common.dsl.SchemaBuilder;

import java.util.Objects;

@VertxGen
public interface Schemas {

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder ifThenElse(SchemaBuilder ifSchema, SchemaBuilder thenSchema, SchemaBuilder elseSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(thenSchema);
    Objects.requireNonNull(elseSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("then", thenSchema::toJson),
        new Keyword("else", elseSchema::toJson)
      );
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder ifThen(SchemaBuilder ifSchema, SchemaBuilder thenSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(thenSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("then", thenSchema::toJson)
      );
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static GenericSchemaBuilder ifElse(SchemaBuilder ifSchema, SchemaBuilder elseSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(elseSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("else", elseSchema::toJson)
      );
  }

}
