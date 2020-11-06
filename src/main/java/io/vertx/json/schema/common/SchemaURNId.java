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

import io.vertx.core.json.pointer.JsonPointer;

import java.net.URI;
import java.util.UUID;

public class SchemaURNId {

  String id;

  public SchemaURNId(String id) {
    if (!id.matches("[a-zA-Z0-9_-]*")) throw new IllegalArgumentException("Id must match pattern [a-zA-Z0-9_-]*");
    this.id = id;
  }

  public SchemaURNId() {
    this.id = UUID.randomUUID().toString();
  }

  public JsonPointer toPointer() {
    return JsonPointer.fromURI(toURI());
  }

  public URI toURI() {
    return URI.create(toString());
  }

  @Override
  public String toString() {
    return "urn:vertxschemas:" + id;
  }
}
