/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
module io.vertx.jsonschema {

  requires com.fasterxml.jackson.core;
  requires transitive io.vertx.core;

  requires static io.vertx.docgen;
  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;

  exports io.vertx.json.schema;
  exports io.vertx.json.schema.common;
  exports io.vertx.json.schema.common.dsl;

  exports io.vertx.json.schema.impl to io.vertx.tests;
  exports io.vertx.json.schema.internal to io.vertx.tests, io.vertx.openapi;

}
