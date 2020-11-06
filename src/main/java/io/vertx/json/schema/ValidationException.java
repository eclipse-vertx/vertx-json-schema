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
import io.vertx.core.json.pointer.JsonPointer;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is the main class for every Validation flow related errors
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
public class ValidationException extends VertxException {

  final private String keyword;
  final private Object input;
  private Schema schema;
  private JsonPointer scope;

  protected ValidationException(String message, String keyword, Object input) {
    super(message);
    this.keyword = keyword;
    this.input = input;
  }

  protected ValidationException(String message, Throwable cause, String keyword, Object input) {
    super(message, cause);
    this.keyword = keyword;
    this.input = input;
  }

  public static ValidationException createException(String message, String keyword, Object input, Collection<Throwable> causes) {
    return createException(message + ". Multiple causes: " + formatExceptions(causes), keyword, input);
  }

  public static ValidationException createException(String message, String keyword, Object input, Throwable cause) {
    return new ValidationException(message, cause, keyword, input);
  }

  public static ValidationException createException(String message, String keyword, Object input) {
    return new ValidationException(message, keyword, input);
  }

  /**
   * Returns the keyword that failed the validation, if any
   *
   * @return
   */
  @Nullable
  public String keyword() {
    return keyword;
  }

  /**
   * Returns the input that triggered the error
   *
   * @return
   */
  public Object input() {
    return input;
  }

  /**
   * Returns the schema that failed the validation
   *
   * @return
   */
  public Schema schema() {
    return schema;
  }

  /**
   * Returns the scope of the schema that failed the validation
   *
   * @return
   */
  public JsonPointer scope() {
    return scope;
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void setScope(JsonPointer scope) {
    this.scope = scope;
  }

  @Override
  public String toString() {
    return "ValidationException{" +
      "message='" + getMessage() + '\'' +
      ", keyword='" + keyword + '\'' +
      ", input=" + input +
      ", schema=" + schema +
      ((scope != null) ? ", scope=" + scope.toURI() : "") +
      '}';
  }

  private static String formatExceptions(Collection<Throwable> throwables) {
    if (throwables == null) {
      return "[]";
    }
    return "[" + throwables
      .stream()
      .filter(Objects::nonNull)
      .map(Throwable::getMessage)
      .collect(Collectors.joining(", ")) + "]";
  }
}
