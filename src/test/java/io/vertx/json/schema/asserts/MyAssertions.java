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
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaRouter;

public class MyAssertions {

  public static SchemaAssert assertThat(Schema actual) {
    return new SchemaAssert(actual);
  }

  public static SchemaRouterAssert assertThat(SchemaRouter actual) {
    return new SchemaRouterAssert(actual);
  }

  public static JsonAssert assertThat(JsonObject actual) {
    return new JsonAssert(actual);
  }

  public static JsonAssert assertThat(JsonArray actual) {
    return new JsonAssert(actual);
  }

  public static JsonAssert assertThatJson(Object actual) {
    return new JsonAssert(actual);
  }

}
