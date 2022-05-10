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
package io.vertx.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaParser;

import java.net.URI;

@Deprecated
public interface SchemaParserInternal extends SchemaParser {

  @Override
  default SchemaInternal parse(JsonObject jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parse(Boolean jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parseFromString(String unparsedJson) {
    return parseFromString(unparsedJson, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parse(JsonObject jsonSchema, JsonPointer schemaPointer) {
    return parse((Object) jsonSchema, schemaPointer);
  }

  @Override
  default SchemaInternal parse(Boolean jsonSchema, JsonPointer schemaPointer) {
    return parse((Object) jsonSchema, schemaPointer);
  }

  SchemaInternal parse(Object jsonSchema, JsonPointer scope, MutableStateValidator parent);

  default SchemaInternal parse(Object jsonSchema, JsonPointer scope) {
    return parse(jsonSchema, scope, null);
  }

  default SchemaInternal parse(Object jsonSchema, URI scope, MutableStateValidator parent) {
    return this.parse(jsonSchema, JsonPointer.fromURI(scope), parent);
  }

  default SchemaInternal parse(Object jsonSchema, URI scope) {
    return parse(jsonSchema, scope, null);
  }

  SchemaInternal parseFromString(String unparsedJson, JsonPointer scope, MutableStateValidator parent);

  default SchemaInternal parseFromString(String unparsedJson, JsonPointer schemaPointer) {
    return parseFromString(unparsedJson, schemaPointer, null);
  }

  default SchemaInternal parseFromString(String unparsedJson, URI scope, MutableStateValidator parent) {
    return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), parent);
  }

  default SchemaInternal parseFromString(String unparsedJson, URI scope) {
    return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), null);
  }


}
