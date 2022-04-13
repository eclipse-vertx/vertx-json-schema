package io.vertx.json.schema.validator.impl;

import io.vertx.json.schema.validator.ValidationError;

public class ErrorUnit implements ValidationError {

  private final String keyword;
  private final String keywordLocation;
  private final String instanceLocation;
  private final String error;

  public ErrorUnit(String instanceLocation, String keyword, String keywordLocation, String error) {
    this.keyword = keyword;
    this.keywordLocation = keywordLocation;
    this.instanceLocation = instanceLocation;
    this.error = error;
  }

  @Override
  public String keyword() {
    return keyword;
  }

  @Override
  public String keywordLocation() {
    return keywordLocation;
  }

  @Override
  public String instanceLocation() {
    return instanceLocation;
  }

  @Override
  public String error() {
    return error;
  }

  @Override
  public String toString() {
    return "{instanceLocation=" + instanceLocation + ", keyword=" + keyword + ", keywordLocation=" + keywordLocation + ", error=" + error + "}";
  }
}
