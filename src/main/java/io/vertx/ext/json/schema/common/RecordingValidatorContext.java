package io.vertx.ext.json.schema.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RecordingValidatorContext implements ValidatorContext {

  Set<Integer> evaluatedItems;
  Set<String> evaluatedProperties;

  @Override
  public ValidatorContext startRecording() {
    return this;
  }

  @Override
  public void markEvaluatedItem(int index) {
    if (evaluatedItems == null) {
      this.evaluatedItems = new HashSet<>();
    }
    evaluatedItems.add(index);
  }

  @Override
  public void markEvaluatedProperty(String propertyName) {
    if (evaluatedProperties == null) {
      this.evaluatedProperties = new HashSet<>();
    }
    evaluatedProperties.add(propertyName);
  }

  @Override
  public Set<Integer> evaluatedItems() {
    return evaluatedItems != null ? evaluatedItems : Collections.emptySet();
  }

  @Override
  public Set<String> evaluatedProperties() {
    return evaluatedProperties != null ? evaluatedProperties : Collections.emptySet();
  }

  @Override
  public ValidatorContext lowerLevelContext() {
    return NoopValidatorContext.getInstance();
  }

}
