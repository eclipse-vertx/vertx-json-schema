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
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@DataObject
@JsonGen(publicConverter = false)
public class OutputUnit {

  private Boolean valid;
  private String absoluteKeywordLocation;
  private String keywordLocation;
  private String instanceLocation;
  private String error;
  private OutputErrorType errorType = OutputErrorType.NONE;

  private List<OutputUnit> errors;
  private List<OutputUnit> annotations;

  public OutputUnit() {
  }

  public OutputUnit(JsonObject json) {
    OutputUnitConverter.fromJson(json, this);
  }

  public OutputUnit(boolean valid) {
    this.valid = valid;
  }

  public OutputUnit(String instanceLocation, String absoluteKeywordLocation, String keywordLocation, String error, OutputErrorType errorType) {
    this.instanceLocation = instanceLocation;
    this.absoluteKeywordLocation = absoluteKeywordLocation;
    this.keywordLocation = keywordLocation;
    this.error = error;
    this.errorType = errorType;
  }

  public Boolean getValid() {
    return valid;
  }

  public OutputUnit setValid(Boolean valid) {
    this.valid = valid;
    return this;
  }

  public String getAbsoluteKeywordLocation() {
    return absoluteKeywordLocation;
  }

  public OutputUnit setAbsoluteKeywordLocation(String absoluteKeywordLocation) {
    this.absoluteKeywordLocation = absoluteKeywordLocation;
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
    return this;
  }

  public List<OutputUnit> getAnnotations() {
    return annotations;
  }

  public OutputUnit setAnnotations(List<OutputUnit> annotations) {
    this.annotations = annotations;
    return this;
  }

  public OutputErrorType getErrorType() {
    return errorType;
  }

  public OutputUnit setErrorType(OutputErrorType errorType) {
    this.errorType = errorType;
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


  public void checkValidity() throws JsonSchemaValidationException {

    final String msg = getError();

    // if valid is null, it means that we are a caused by error
    if (valid == null) {
      final String location = getAbsoluteKeywordLocation();

      throw new JsonSchemaValidationException(
        msg == null ? "JsonSchema Validation error" : msg,
        location,
        errorType,
        // add some information to the stack trace
        createStackTraceElement());
    } else {
      if (!valid) {
        // valid is "false" we need to throw an exception
        if (errors == null || errors.isEmpty()) {
          final String location = getAbsoluteKeywordLocation();

          // there are no sub errors, but the validation failed
          throw new JsonSchemaValidationException(
            msg == null ? "JsonSchema Validation error" : msg,
            location,
            errorType,
            // add some information to the stack trace
            createStackTraceElement());
        } else {
          // there are sub errors, we need to cycle them and create a chain of exceptions
          JsonSchemaValidationException lastException = null;
          for (final OutputUnit error : errors) {
            final String location = error.getAbsoluteKeywordLocation();

            JsonSchemaValidationException cause;
            cause = new JsonSchemaValidationException(
              error.getError(),
              lastException,
              location,
              errorType,
              // add some information to the stack trace
              error.createStackTraceElement());
            lastException = cause;
          }
          if (msg == null) {
            throw lastException;
          } else {
            // one final wrap as there is extra error message in the unit
            throw new JsonSchemaValidationException(msg, lastException, getAbsoluteKeywordLocation(), errorType);
          }
        }
      }
    }
  }

  private StackTraceElement createStackTraceElement() {
    if (instanceLocation == null && keywordLocation == null) {
      return null;
    }
    return new StackTraceElement("[" + keywordLocation + "]", "<" + instanceLocation + ">", absoluteKeywordLocation, -1);
  }

  /**
   * @TODO: required for validation/openapi. In those modules errors are handled as typed exceptions
   */
  @GenIgnore
  public ValidationException toException(Object input) {
    return new ValidationException(error + ": { errors: " + formatExceptions(errors) + ", annotations: " + formatExceptions(annotations) + "}", absoluteKeywordLocation, input, true) {
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
