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
package io.vertx.ext.json.schema.asserts;

import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaParser;
import io.vertx.ext.json.schema.SchemaRouter;
import io.vertx.ext.json.schema.common.SchemaImpl;
import io.vertx.ext.json.schema.common.URIUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SchemaRouterAssert extends AbstractAssert<SchemaRouterAssert, SchemaRouter> {

  public SchemaRouterAssert(SchemaRouter actual) {
    super(actual, SchemaRouterAssert.class);
  }

  public SchemaAssert canResolveSchema(String uri, JsonPointer scope, SchemaParser parser) {
    return canResolveSchema(URIUtils.createJsonPointerFromURI(URI.create(uri)), scope, parser);
  }

  public SchemaAssert canResolveSchema(JsonPointer jp, JsonPointer scope, SchemaParser parser) {
    isNotNull();

    try {
      Schema s = actual.resolveCachedSchema(jp, scope, parser);
      assertThat(s)
        .withFailMessage("Cannot resolve schema with pointer '%s' from scope '%s'", jp.toURI(), scope.toURI())
        .isNotNull();
      return new SchemaAssert(actual.resolveCachedSchema(jp, scope, parser));
    } catch (Exception e) {
      fail(String.format("Cannot resolve schema with pointer '%s' from scope '%s'", jp.toURI(), scope.toURI()), e);
      return new SchemaAssert(null);
    }
  }

  public SchemaRouterAssert cannotResolveSchema(String uri, JsonPointer scope, SchemaParser parser) {
    return cannotResolveSchema(URIUtils.createJsonPointerFromURI(URI.create(uri)), scope, parser);
  }

  public SchemaRouterAssert cannotResolveSchema(JsonPointer jp, JsonPointer scope, SchemaParser parser) {
    isNotNull();
    assertThat(actual.resolveCachedSchema(jp, scope, parser)).isNull();
    return this;
  }

  public SchemaRouterAssert containsOnlyOneCachedSchemaWithXId(String expectedXId) {
    isNotNull();

    assertThat(actual.registeredSchemas())
      .isNotNull();

    assertThat(actual.registeredSchemas())
      .filteredOn(s -> s instanceof SchemaImpl)
      .extracting(s -> ((SchemaImpl) s).getJson())
      .extracting(j -> j.getString("x-id"))
      .areExactly(1, new Condition<>(expectedXId::equals, "Expected id {}", expectedXId));


    return this;
  }

  public SchemaRouterAssert containsCachedSchemasWithXIds(String... expectedXIds) {
    for (String id : expectedXIds) {
      containsOnlyOneCachedSchemaWithXId(id);
    }

    return this;
  }

}
