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

/**
 * This is the main class for every JsonSchemaValidation flow related errors
 */
public final class JsonSchemaValidationException extends Exception {

  final private String location;
  final private OutputErrorType errorType;

  public JsonSchemaValidationException(String message, Throwable cause, String location, OutputErrorType errorType) {
    super(message, cause);
    this.location = location;
    this.errorType = errorType;
  }

  public JsonSchemaValidationException(String message, String location, OutputErrorType errorType,
                                       StackTraceElement stackTraceElement) {
    this(message, null, location, errorType, stackTraceElement);
  }

  public JsonSchemaValidationException(String message, Throwable cause, String location, OutputErrorType errorType,
                                       StackTraceElement stackTraceElement) {
    super(message, cause, stackTraceElement != null, stackTraceElement != null);
    this.location = location;
    this.errorType = errorType;
    if (stackTraceElement != null) {
      setStackTrace(new StackTraceElement[]{
        stackTraceElement
      });
    }
  }

  /**
   * @return the location that failed the validation, if any
   */
  @Nullable
  public String location() {
    return location;
  }

  /**
   * @return our best guess on what the validation error type is.
   */
  public OutputErrorType errorType() {
    return errorType;
  }

}
