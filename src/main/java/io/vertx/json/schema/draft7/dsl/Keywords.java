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

import io.vertx.json.schema.common.dsl.*;

import java.util.Objects;

public class Keywords extends io.vertx.json.schema.common.dsl.Keywords {

  public static NumberKeyword exclusiveMaximum(double exclusiveMaximum) {
    return new NumberKeyword("exclusiveMaximum", exclusiveMaximum);
  }

  public static NumberKeyword maximum(double maximum) {
    return new NumberKeyword("maximum", maximum);
  }

  public static NumberKeyword exclusiveMinimum(double exclusiveMinimum) {
    return new NumberKeyword("exclusiveMinimum", exclusiveMinimum);
  }

  public static NumberKeyword minimum(double minimum) {
    return new NumberKeyword("minimum", minimum);
  }

  public static StringKeyword format(StringFormat format) {
    Objects.requireNonNull(format);
    return new StringKeyword("format", format.getName());
  }

  public static ArrayKeyword contains(SchemaBuilder builder) {
    Objects.requireNonNull(builder);
    return new ArrayKeyword("contains", builder::toJson);
  }

  public static ObjectKeyword propertyNames(StringSchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    return new ObjectKeyword("propertyNames", schemaBuilder::toJson);
  }

}
