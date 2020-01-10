package io.vertx.ext.json.schema.common;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.json.pointer.JsonPointerIterator;
import io.vertx.ext.json.schema.Schema;

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
    return !isNull(value) && ((RouterNode)value).getChilds().containsKey(key);
  }

  @Override
  public Object getObjectParameter(Object value, String key, boolean createOnMissing) {
    if (isObject(value)) {
      if (!objectContainsKey(value, key) && createOnMissing) {
        if (createOnMissing) {
          RouterNode node = new RouterNode();
          ((RouterNode)value).getChilds().put(key, node);
        } else {
          return null;
        }
      }
      return ((RouterNode)value).getChilds().get(key);
    }
    return null;
  }

  @Override
  public Object getArrayElement(Object value, int i) {
    return null;
  }

  @Override
  public boolean writeObjectParameter(Object value, String key, Object newElement) {
    if (newElement instanceof Schema) {
      value = this.getObjectParameter(value, key, true);
      if (value != null)
        ((RouterNode)value).setSchema((Schema) newElement);
      return true;
    } else if (newElement instanceof RouterNode) {
      ((RouterNode)value).getChilds().put(key, (RouterNode) newElement);
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
