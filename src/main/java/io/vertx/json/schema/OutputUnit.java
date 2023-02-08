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
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  /**
   * @TODO: required for validation/openapi. In those modules errors are handled as typed exceptions
   */
  @GenIgnore
  public ValidationException toException(Object input) {

//    StackTraceElement[] stackTrace = new StackTraceElement[errors.size()];
//    int len = errors.size() - 1;
//    for (int i = 0; i < errors.size(); i++) {
//      OutputUnit error = errors.get(len - i);
//      stackTrace[i] = new StackTraceElement(error.getError(), error.getKeywordLocation(), error.getInstanceLocation(), -1);
//    }

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
