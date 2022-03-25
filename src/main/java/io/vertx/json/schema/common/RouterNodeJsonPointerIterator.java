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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.json.pointer.JsonPointerIterator;

class RouterNodeJsonPointerIterator implements JsonPointerIterator {

  public final static RouterNodeJsonPointerIterator INSTANCE = new RouterNodeJsonPointerIterator();

  @Override
  public boolean isObject(Object value) {
    return !isNull(value);
  }

  @Override
  public boolean isArray(Object value) {
    return false;
  }

  @Override
  public boolean isNull(Object value) {
    return value == null;
  }

  @Override
  public boolean objectContainsKey(Object value, String key) {
    return !isNull(value) && ((RouterNode) value).getChilds().containsKey(key);
  }

  @Override
  public Object getObjectParameter(Object value, String key, boolean createOnMissing) {
    if (isObject(value)) {
      if (!objectContainsKey(value, key)) {
        if (createOnMissing) {
          RouterNode node = new RouterNode();
          ((RouterNode) value).getChilds().put(key, node);
        } else {
          return null;
        }
      }
      return ((RouterNode) value).getChilds().get(key);
    }
    return null;
  }

  @Override
  public Object getArrayElement(Object value, int i) {
    return null;
  }

  @Override
  public boolean writeObjectParameter(Object value, String key, Object newElement) {
    if (newElement instanceof SchemaInternal) {
      value = this.getObjectParameter(value, key, true);
      if (value != null)
        ((RouterNode) value).setSchema((SchemaInternal) newElement);
      return true;
    } else if (newElement instanceof RouterNode) {
      ((RouterNode) value).getChilds().put(key, (RouterNode) newElement);
      return true;
    }
    return false;
  }

  @Override
  public boolean writeArrayElement(Object value, int i, @Nullable Object newElement) {
    return false;
  }

  @Override
  public boolean appendArrayElement(Object value, Object newElement) {
    return false;
  }

}
