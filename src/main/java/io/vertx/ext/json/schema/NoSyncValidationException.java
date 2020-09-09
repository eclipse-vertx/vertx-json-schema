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
package io.vertx.ext.json.schema;

import io.vertx.core.VertxException;
import io.vertx.ext.json.schema.common.MutableStateValidator;

/**
 * This exception is thrown when you call {@link Schema#validateSync(Object)} when the schema is in an asynchronous state
 */
public class NoSyncValidationException extends VertxException {

  private MutableStateValidator validator;

  public NoSyncValidationException(String message, MutableStateValidator validator) {
    super(message);
    this.validator = validator;
  }

  public NoSyncValidationException(String message, Throwable cause, MutableStateValidator validator) {
    super(message, cause);
    this.validator = validator;
  }

  public MutableStateValidator getValidator() {
    return validator;
  }
}
