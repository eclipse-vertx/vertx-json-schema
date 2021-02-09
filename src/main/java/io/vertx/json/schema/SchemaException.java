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

import io.vertx.core.VertxException;

/**
 * This class represents an error while parsing a {@link Schema}
 *
 * @author slinkydeveloper
 */
public class SchemaException extends VertxException {

  private Object schema;

  public SchemaException(Object schema, String message, Throwable cause) {
    super(message, cause);
    this.schema = schema;
  }

  public SchemaException(Object schema, String message) {
    super(message);
    this.schema = schema;
  }

  /**
   * Json representation of the schema
   *
   * @return
   */
  public Object schema() {
    return schema;
  }

  @Override
  public String toString() {
    return "SchemaException{" +
      "message=\'" + getMessage() + "\'" +
      ", schema=" + schema +
      '}';
  }
}
