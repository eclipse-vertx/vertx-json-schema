package io.vertx.json.schema.validator.impl;

import io.vertx.json.schema.validator.ValidationError;
import io.vertx.json.schema.validator.ValidationResult;

import java.util.Collections;
import java.util.List;

public class ValidationResultImpl implements ValidationResult {

  private final boolean valid;
  private final List<ValidationError> errors;

  public ValidationResultImpl(List<ValidationError> errors) {
    this(errors.isEmpty(), errors);
  }

  public ValidationResultImpl(boolean valid, List<ValidationError> errors) {
    this.valid = valid;
    this.errors = Collections.unmodifiableList(errors);
  }

  @Override
  public boolean valid() {
    return valid;
  }

  @Override
  public List<ValidationError> errors() {
    return errors;
  }

  @Override
  public String toString() {
    return "{valid=" + valid + ", errors=" + errors.toString() + "}";
  }
}
