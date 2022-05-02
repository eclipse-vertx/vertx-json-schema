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
import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * Configuration options for the Json-Schema validator
 *
 * @author Paulo Lopes
 */
@DataObject(generateConverter = true)
public class JsonSchemaOptions {

  /**
   * Your application base uri.
   */
  private String baseUri;

  /**
   * Which draft to use during validation.
   */
  private Draft draft;

  /**
   * Which output format to use during validation.
   */
  private OutputFormat outputFormat = OutputFormat.Flag;

  public JsonSchemaOptions() {
  }

  public JsonSchemaOptions(JsonObject json) {
    JsonSchemaOptionsConverter.fromJson(json, this);
  }

  public JsonSchemaOptions(JsonSchemaOptions other) {
    Objects.requireNonNull(other, "'other' cannot be null");
    this.baseUri = other.baseUri;
    this.draft = other.draft;
    this.outputFormat = other.outputFormat;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public JsonSchemaOptions setBaseUri(String baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  public Draft getDraft() {
    return draft;
  }

  public JsonSchemaOptions setDraft(Draft draft) {
    this.draft = draft;
    return this;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public JsonSchemaOptions setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
    return this;
  }

  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    JsonSchemaOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
