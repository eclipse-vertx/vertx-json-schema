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

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Keywords {

  public static Keyword type(SchemaType... types) {
    Objects.requireNonNull(types);
    if (types.length == 1)
      return new Keyword("type", types[0].getName());
    else
      return new Keyword("type", Arrays.stream(types).map(SchemaType::getName).collect(Collectors.toList()));
  }

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

  public static NumberKeyword multipleOf(double multipleOf) {
    return new NumberKeyword("multipleOf", multipleOf);
  }

  public static StringKeyword format(StringFormat format) {
    return new StringKeyword("format", format.getName());
  }

  public static StringKeyword maxLength(int maxLength) {
    return new StringKeyword("maxLength", maxLength);
  }

  public static StringKeyword minLength(int minLength) {
    return new StringKeyword("minLength", minLength);
  }

  public static StringKeyword pattern(Pattern pattern) {
    Objects.requireNonNull(pattern);
    return new StringKeyword("pattern", pattern.toString());
  }

  public static ArrayKeyword maxItems(int maxItems) {
    return new ArrayKeyword("maxItems", maxItems);
  }

  public static ArrayKeyword minItems(int minItems) {
    return new ArrayKeyword("minItems", minItems);
  }

  public static ArrayKeyword uniqueItems() {
    return new ArrayKeyword("uniqueItems", true);
  }

  public static ObjectKeyword maxProperties(int maxProperties) {
    return new ObjectKeyword("maxProperties", maxProperties);
  }

  public static ObjectKeyword minProperties(int minProperties) {
    return new ObjectKeyword("minProperties", minProperties);
  }

}
