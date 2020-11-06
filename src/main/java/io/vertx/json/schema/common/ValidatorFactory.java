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
import io.vertx.json.schema.SchemaException;

/**
 * Factory for {@link Validator}. This is the entrypoint if you want to create a new custom keyword
 */
public interface ValidatorFactory {
  /**
   * This method consume the schema eventually creating a new {@link Validator}. The schema parser calls it during schema parsing only if {@link #canConsumeSchema(JsonObject)} returns true <br/>
   * <p>
   * You can return any of {@link SyncValidator}, {@link AsyncValidator} or {@link MutableStateValidator}
   *
   * @param schema JsonObject representing the schema
   * @param scope  scope of the parsed schema
   * @param parser caller parser
   * @param parent parent of this schema
   * @return the created validator.
   * @throws SchemaException if the keyword(s) handled by this ValidatorFactory are invalid
   */
  Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) throws SchemaException;

  /**
   * Returns true if this factory can consume the provided schema, eventually returning an instance of {@link Validator}
   *
   * @param schema
   * @return
   */
  boolean canConsumeSchema(JsonObject schema);
}
