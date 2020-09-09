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
