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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.VertxException;

/**
 * This is the main class for every JsonSchemaValidation flow related errors
 */
public final class JsonSchemaValidationException extends VertxException {

  final private String location;

  public JsonSchemaValidationException(String message, Throwable cause, String location) {
    super(message, cause);
    this.location = location;
  }

  public JsonSchemaValidationException(String message, String location, StackTraceElement stackTraceElement) {
    this(message, null, location, stackTraceElement);
  }

  public JsonSchemaValidationException(String message, Throwable cause, String location, StackTraceElement stackTraceElement) {
    super(message, cause, false);
    this.location = location;
    setStackTrace(new StackTraceElement[]{stackTraceElement});
  }

  /**
   * @return the location that failed the validation, if any
   */
  @Nullable
  public String location() {
    return location;
  }
}
