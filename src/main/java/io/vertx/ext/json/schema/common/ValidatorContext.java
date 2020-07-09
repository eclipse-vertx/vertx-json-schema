package io.vertx.ext.json.schema.common;

import java.util.Set;

/**
 * Validator context is an interface used to process contextual keywords (like unevaluatedProperties, unevaluatedItems)
 */
public interface ValidatorContext {

  void markEvaluatedItem(int index);

  void markEvaluatedProperty(String propertyName);

  Set<Integer> evaluatedItems();

  Set<String> evaluatedProperties();

  ValidatorContext startRecording();

  ValidatorContext lowerLevelContext();

}
