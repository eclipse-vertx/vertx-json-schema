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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.impl.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class OutputUnit {

  private Boolean valid;
  private String keyword;
  private String keywordLocation;
  private String instanceLocation;
  private String error;

  private List<OutputUnit> errors;
  private List<OutputUnit> annotations;

  private String schemaLocation;

  public OutputUnit() {
    valid = true;
  }

  public OutputUnit(JsonObject json) {
    OutputUnitConverter.fromJson(json, this);
  }

  public OutputUnit(boolean valid) {
    this.valid = valid;
  }

  public OutputUnit(String instanceLocation, String keyword, String keywordLocation, String error) {
    this.instanceLocation = instanceLocation;
    this.keyword = keyword;
    this.keywordLocation = keywordLocation;
    this.error = error;
  }

  public Boolean getValid() {
    return valid;
  }

  public OutputUnit setValid(Boolean valid) {
    this.valid = valid;
    return this;
  }

  public String getKeyword() {
    return keyword;
  }

  public OutputUnit setKeyword(String keyword) {
    this.keyword = keyword;
    return this;
  }

  public String getKeywordLocation() {
    return keywordLocation;
  }

  public OutputUnit setKeywordLocation(String keywordLocation) {
    this.keywordLocation = keywordLocation;
    return this;
  }

  public String getInstanceLocation() {
    return instanceLocation;
  }

  public OutputUnit setInstanceLocation(String instanceLocation) {
    this.instanceLocation = instanceLocation;
    return this;
  }

  public String getError() {
    return error;
  }

  public OutputUnit setError(String error) {
    this.error = error;
    return this;
  }

  public List<OutputUnit> getErrors() {
    return errors;
  }

  public OutputUnit setErrors(List<OutputUnit> errors) {
    this.errors = errors;
    return this;
  }

  public OutputUnit addError(OutputUnit error) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
      this.valid = false;
    }
    this.errors.add(error);
    return this;
  }

  @GenIgnore
  public OutputUnit addErrors(List<OutputUnit> errors) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
      this.valid = false;
    }
    this.errors.addAll(errors);
    return this;
  }

  public List<OutputUnit> getAnnotations() {
    return annotations;
  }

  public OutputUnit setAnnotations(List<OutputUnit> annotations) {
    this.annotations = annotations;
    return this;
  }

  public OutputUnit addAnnotation(OutputUnit annotation) {
    if (this.annotations == null) {
      this.annotations = new ArrayList<>();
    }
    this.annotations.add(annotation);
    return this;
  }

  @GenIgnore
  public OutputUnit addAnnotations(List<OutputUnit> annotations) {
    if (this.annotations == null) {
      this.annotations = new ArrayList<>();
    }
    this.annotations.addAll(annotations);
    return this;
  }

  @GenIgnore
  public OutputUnit setSchemaLocation(String schemaLocation) {
    this.schemaLocation = schemaLocation;
    return this;
  }

  public void checkValidity() throws VertxException {
    final URL baseUri = schemaLocation != null ? new URL(schemaLocation) : null;
    final VertxException exception;
    String msg = getError();

    final Function<String, String> urlFormatter = fragment -> {
      if (baseUri == null) {
        return fragment;
      }
      return new URL(fragment, baseUri).href();
    };


    // if valid is null, it means that we are a caused by error
    if (valid == null) {
      exception = new VertxException(msg == null ? "JsonSchema Validation error" : msg, false);
      // add some information to the stack trace
      exception.setStackTrace(
        new StackTraceElement[]{
          new StackTraceElement("[" + urlFormatter.apply(getInstanceLocation()) + "]", "<" + getKeyword() + ">", getKeywordLocation(), -1)
        }
      );
    } else {
      if (!valid) {
        // valid is "false" we need to throw an exception

        if (errors.isEmpty()) {
          // there are no sub errors, but the validation failed
          exception = new VertxException(msg == null ? "JsonSchema Validation error" : msg, false);
          // add some information to the stack trace
          exception.setStackTrace(
            new StackTraceElement[]{
              new StackTraceElement("[" + urlFormatter.apply(getInstanceLocation()) + "]", "<" + getKeyword() + ">", getKeywordLocation(), -1)
            }
          );
        } else {
          // there are sub errors, we need to cycle them and create a chain of exceptions
          VertxException lastException = null;
          for (int i = errors.size() - 1; i >= 0; i--) {
            final OutputUnit error = errors.get(i);
            VertxException cause;
            if (lastException == null) {
              cause = new VertxException(error.getError(), false);
            } else {
              cause = new VertxException(error.getError(), lastException, false);
            }
            // add some information to the stack trace
            cause.setStackTrace(
              new StackTraceElement[]{
                new StackTraceElement("[" + urlFormatter.apply(error.getInstanceLocation()) + "]", "<" + error.getKeyword() + ">", error.getKeywordLocation(), -1)
              }
            );
            lastException = cause;
          }
          exception = new VertxException(msg == null ? "JsonSchema Validation error" : msg, lastException, false);
        }

        throw exception;
      }
    }

  }

  /**
   * @TODO: required for validation/openapi. In those modules errors are handled as typed exceptions
   */
  @GenIgnore
  public ValidationException toException(Object input) {
    return new ValidationException(error + ": { errors: " + formatExceptions(errors) + ", annotations: " + formatExceptions(annotations) + "}", keyword, input, true) {
    };
  }

  private String formatExceptions(List<OutputUnit> units) {
    if (units == null) {
      return "[]";
    }
    return
      "[" +
        units
          .stream()
          .filter(Objects::nonNull)
          .map(OutputUnit::toString)
          .collect(Collectors.joining(", ")) +
        "]";

  }

  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    OutputUnitConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
