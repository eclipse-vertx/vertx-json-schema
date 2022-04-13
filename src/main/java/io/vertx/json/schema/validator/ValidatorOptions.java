package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ValidatorOptions {
  private String baseUri = "https://vertx.io";
  private Draft draft = Draft.DRAFT201909;
  private boolean shortCircuit = true;

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

  public boolean isShortCircuit() {
    return shortCircuit;
  }

  public ValidatorOptions setShortCircuit(boolean shortCircuit) {
    this.shortCircuit = shortCircuit;
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
