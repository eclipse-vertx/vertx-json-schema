package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ValidatorOptions {
  private String baseUri;
  private Draft draft;
  private OutputFormat outputFormat = OutputFormat.Flag;

  public ValidatorOptions() {
  }

  public ValidatorOptions(JsonObject json) {
    ValidatorOptionsConverter.fromJson(json, this);
  }

  public String getBaseUri() {
    return baseUri;
  }

  public ValidatorOptions setBaseUri(String baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  public Draft getDraft() {
    return draft;
  }

  public ValidatorOptions setDraft(Draft draft) {
    this.draft = draft;
    return this;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public ValidatorOptions setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
    return this;
  }

  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    ValidatorOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encodePrettily();
  }
}
