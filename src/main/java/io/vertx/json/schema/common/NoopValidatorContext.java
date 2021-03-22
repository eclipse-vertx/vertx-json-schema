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
package io.vertx.json.schema.common;

import java.util.Collections;
import java.util.Set;

/**
 * This noop {@link ValidatorContext} can be used when no contextual keywords are used
 */
public class NoopValidatorContext implements ValidatorContext {

  final private ValidatorContext parent;
  final private String inputKey;

  public NoopValidatorContext() {
    this.parent = null;
    this.inputKey = null;
  }

  public NoopValidatorContext(ValidatorContext parent, String inputKey) {
    this.parent = parent;
    this.inputKey = inputKey;
  }

  @Override
  public ValidatorContext startRecording() {
    return new RecordingValidatorContext(parent, inputKey);
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
  public ValidatorContext lowerLevelContext(String key) {
    return new NoopValidatorContext(this, key);
  }

  @Override
  public ValidatorContext lowerLevelContext(int key) {
    return new NoopValidatorContext(this, Integer.toString(key));
  }

  @Override
  public ValidatorContext parent() {
    return parent;
  }

  @Override
  public String inputKey() {
    return inputKey;
  }
}
