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

import io.vertx.core.json.pointer.JsonPointer;

import java.util.Set;

public class RecursiveAnchorValidatorContextDecorator implements ValidatorContext {

  private final ValidatorContext context;
  private final JsonPointer recursiveAnchor;

  private RecursiveAnchorValidatorContextDecorator(ValidatorContext context, JsonPointer recursiveAnchor) {
    this.context = context;
    this.recursiveAnchor = recursiveAnchor;
  }

  @Override
  public void markEvaluatedItem(int index) {
    this.context.markEvaluatedItem(index);
  }

  @Override
  public void markEvaluatedProperty(String propertyName) {
    this.context.markEvaluatedProperty(propertyName);
  }

  @Override
  public Set<Integer> evaluatedItems() {
    return this.context.evaluatedItems();
  }

  @Override
  public Set<String> evaluatedProperties() {
    return this.context.evaluatedProperties();
  }

  @Override
  public ValidatorContext startRecording() {
    return wrapNewContext(this.context.startRecording());
  }

  @Override
  public ValidatorContext lowerLevelContext() {
    return wrapNewContext(this.context.lowerLevelContext());
  }

  public JsonPointer getRecursiveAnchor() {
    return this.recursiveAnchor;
  }

  public ValidatorContext unwrap() {
    return this.context;
  }

  private ValidatorContext wrapNewContext(ValidatorContext newContext) {
    if (newContext == this.context) {
      return this;
    }
    return new RecursiveAnchorValidatorContextDecorator(newContext, this.recursiveAnchor);
  }

  public static ValidatorContext wrap(ValidatorContext context, JsonPointer recursiveAnchor) {
    if (context instanceof RecursiveAnchorValidatorContextDecorator) {
      return context;
    } else {
      return new RecursiveAnchorValidatorContextDecorator(context, recursiveAnchor);
    }
  }

  public static ValidatorContext unwrap(ValidatorContext context) {
    if (context instanceof RecursiveAnchorValidatorContextDecorator) {
      return ((RecursiveAnchorValidatorContextDecorator) context).unwrap();
    } else {
      return context;
    }
  }
}
