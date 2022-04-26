package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@DataObject(generateConverter = true)
public class OutputUnit {

  private Boolean valid;
  private String keyword;
  private String keywordLocation;
  private String instanceLocation;
  private String error;

  private List<OutputUnit> errors;
  private List<OutputUnit> annotations;

  public OutputUnit() {}

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
    }
    this.errors.add(error);
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

  public JsonObject toJson() {
    final JsonObject json = new JsonObject();
    OutputUnitConverter.toJson(this, json);
    return json;
  }
}
