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
