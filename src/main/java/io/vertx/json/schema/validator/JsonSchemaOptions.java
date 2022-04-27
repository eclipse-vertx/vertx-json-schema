package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

@DataObject(generateConverter = true)
public class JsonSchemaOptions {
  private String baseUri;
  private Draft draft;
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
