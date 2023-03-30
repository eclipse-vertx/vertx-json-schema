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
package io.vertx.json.schema;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.impl.JsonRef;

/**
 * JsonObject {@code $ref} resolver. This interface is used to resolve {@code $ref} in a {@link JsonObject}. The result
 * is a new {@link JsonObject} with all the {@code $ref} replaced by actual object references.
 *
 * This new object allows navigation and queries using {@link io.vertx.core.json.pointer.JsonPointer} but will not be
 * able to be encoded back to JSON when circular dependencies are present.
 *
 * The resolver will only resolve local references as defined in RFC3986. A local reference is a reference that starts
 * with {@code #} and is followed by a valid JSON Pointer.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface Ref {

  /**
   * Resolve all {@code $ref} in the given {@link JsonObject}. The resolution algrithm is not aware of other
   * specifications. When resolving OpenAPI documents (which only allow {@code $ref} at specific locations) you
   * should validate if the document is valid before performing a resolution.
   *
   * It is important to note that any sibling elements of a {@code $ref} is ignored. This is because {@code $ref}
   * works by replacing itself and everything on its level with the definition it is pointing at.
   *
   * @param json the JSON object to resolve.
   * @return a new JSON object with all the {@code $ref} replaced by actual object references.
   * @throws IllegalArgumentException when the input JSON is not valid.
   * @throws UnsupportedOperationException reducing the JSON pointer to a value is undefined.
   */
  static JsonObject resolve(JsonObject json) {
    return JsonRef.resolve(json);
  }
}
