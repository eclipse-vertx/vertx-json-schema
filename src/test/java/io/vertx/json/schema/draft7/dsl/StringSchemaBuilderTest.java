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

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.vertx.json.schema.TestUtils.entry;
import static io.vertx.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.*;

public class StringSchemaBuilderTest {

  @Test
  public void testKeywords() {
    assertThat(
      stringSchema()
        .with(maxLength(10), minLength(1), pattern(Pattern.compile("[a-zA-Z]*")), format(StringFormat.REGEX))
        .toJson()
    ).removingEntry("$id")
      .containsAllAndOnlyEntries(
        entry("type", "string"),
        entry("maxLength", 10),
        entry("minLength", 1),
        entry("pattern", Pattern.compile("[a-zA-Z]*").toString()),
        entry("format", "regex")
      );
  }

}
