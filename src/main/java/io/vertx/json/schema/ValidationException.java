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
import io.vertx.json.schema.common.ValidationExceptionImpl;

import java.util.Collection;

/**
 * This is the main class for every Validation flow related errors
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
public abstract class ValidationException extends VertxException {

  final private String keyword;
  final private Object input;
  protected JsonPointer inputScope;

  protected ValidationException(String message, String keyword, Object input) {
    this(message, null, keyword, input);
  }

  protected ValidationException(String message, String keyword, Object input, boolean noStackTrace) {
    this(message, null, keyword, input, noStackTrace);
  }

  protected ValidationException(String message, Throwable cause, String keyword, Object input) {
    super(message, cause);
    this.keyword = keyword;
    this.input = input;
  }

  protected ValidationException(String message, Throwable cause, String keyword, Object input, boolean noStackTrace) {
    super(message, cause, noStackTrace);
    this.keyword = keyword;
    this.input = input;
  }

  /**
   * @deprecated just use {@link #create(String, String, Object, Collection)}
   */
  @Deprecated
  public static ValidationException createException(String message, String keyword, Object input, Collection<Throwable> causes) {
    return new ValidationExceptionImpl(message, causes, keyword, input);
  }

  /**
   * @deprecated just use {@link #create(String, String, Object, Throwable)}
   */
  @Deprecated
  public static ValidationException createException(String message, String keyword, Object input, Throwable cause) {
    return new ValidationExceptionImpl(message, cause, keyword, input);
  }

  /**
   * @deprecated just use {@link #create(String, String, Object)}
   */
  @Deprecated
  public static ValidationException createException(String message, String keyword, Object input) {
    return new ValidationExceptionImpl(message, keyword, input);
  }

  public static ValidationException create(String message, String keyword, Object input, Collection<Throwable> causes) {
    return new ValidationExceptionImpl(message, causes, keyword, input);
  }

  public static ValidationException create(String message, String keyword, Object input, Throwable cause) {
    return new ValidationExceptionImpl(message, cause, keyword, input);
  }

  public static ValidationException create(String message, String keyword, Object input) {
    return new ValidationExceptionImpl(message, keyword, input);
  }

  /**
   * @return the keyword that failed the validation, if any
   */
  @Nullable
  public String keyword() {
    return keyword;
  }

  /**
   * @return the input that triggered the error
   */
  public Object input() {
    return input;
  }

  /**
   * @return the scope of the input, where the validation failed.
   */
  public JsonPointer inputScope() {
    return this.inputScope;
  }

  @Override
  public String toString() {
    return "ValidationException{" +
      "message='" + getMessage() + '\'' +
      ", keyword='" + keyword + '\'' +
      ", input=" + input +
      ((inputScope != null) ? ", inputScope=" + inputScope.toURI() : "") +
      '}';
  }

}
