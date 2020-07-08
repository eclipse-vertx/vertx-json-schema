package io.vertx.ext.json.schema.common;

import java.util.Collections;
import java.util.Set;

/**
 * This noop {@link ValidatorContext} can be used when no contextual keywords are used
 */
public class NoopValidatorContext implements ValidatorContext {

  private static class NoopValidatorContextHolder {
    static final NoopValidatorContext INSTANCE = new NoopValidatorContext();
  }

  public static NoopValidatorContext getInstance() {
    return NoopValidatorContext.NoopValidatorContextHolder.INSTANCE;
  }

  @Override
  public ValidatorContext startRecording() {
    return new RecordingValidatorContext();
  }

  @Override
  public void markEvaluatedItem(int index) {
  }

  @Override
  public void markEvaluatedProperty(String propertyName) {
  }

  @Override
  public Set<Integer> evaluatedItems() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> evaluatedProperties() {
    return Collections.emptySet();
  }

  @Override
  public ValidatorContext lowerLevelContext() {
    return this;
  }
}
