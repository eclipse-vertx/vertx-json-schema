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
package io.vertx.json.schema.impl;

public abstract class JsonFormatValidator {

  /**
   *
   * @param format The format specified in the schema for the current object instance.
   * @param instance The current object instance that is currently being validated.
   * @return Any string if there are any format validation errors, null if there are no validation errors.
   */
  public abstract String validateFormat(String format, Object instance);

}
