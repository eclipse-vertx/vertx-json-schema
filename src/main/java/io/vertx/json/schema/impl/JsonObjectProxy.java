package io.vertx.json.schema.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import io.vertx.core.json.JsonObject;

import java.io.StringWriter;
import java.util.Objects;

public class JsonObjectProxy extends JsonObject implements JsonProxyEncoder {
  public JsonObjectProxy() {
  }

  public JsonObjectProxy(JsonObject json) {
    super(json.getMap());
  }

  @Override
  public String encode() {
    StringWriter sw = new StringWriter();
    JsonGenerator generator = createGenerator(sw, false);
    return encode(sw, generator);
  }

  @Override
  public String encodePrettily() {
    StringWriter sw = new StringWriter();
    JsonGenerator generator = createGenerator(sw, true);
    return encode(sw, generator);
  }

  @Override
  public boolean equals(Object o) { // copied from vertx-core
    // null check
    if (o == null)
      return false;
    // self check
    if (this == o)
      return true;
    // type check and cast
    if (!(o instanceof JsonObject)) // This is important otherwise our tests will fail.
      return false;

    JsonObject other = (JsonObject) o;
    // size check
    if (this.size() != other.size())
      return false;
    // value comparison
    for (String key : getMap().keySet()) {
      if (!other.containsKey(key)) {
        return false;
      }

      Object thisValue = this.getValue(key);
      Object otherValue = other.getValue(key);
      // identity check
      if (thisValue == otherValue) {
        continue;
      }
      // special case for numbers
      if (thisValue instanceof Number && otherValue instanceof Number && thisValue.getClass() != otherValue.getClass()) {
        Number n1 = (Number) thisValue;
        Number n2 = (Number) otherValue;
        // floating point values
        if (thisValue instanceof Float || thisValue instanceof Double || otherValue instanceof Float || otherValue instanceof Double) {
          // compare as floating point double
          if (n1.doubleValue() == n2.doubleValue()) {
            // same value check the next entry
            continue;
          }
        }
        if (thisValue instanceof Integer || thisValue instanceof Long || otherValue instanceof Integer || otherValue instanceof Long) {
          // compare as integer long
          if (n1.longValue() == n2.longValue()) {
            // same value check the next entry
            continue;
          }
        }
      }
      // special case for char sequences
      if (thisValue instanceof CharSequence && otherValue instanceof CharSequence && thisValue.getClass() != otherValue.getClass()) {
        CharSequence s1 = (CharSequence) thisValue;
        CharSequence s2 = (CharSequence) otherValue;

        if (Objects.equals(s1.toString(), s2.toString())) {
          // same value check the next entry
          continue;
        }
      }
      // fallback to standard object equals checks
      if (!Objects.equals(thisValue, otherValue)) {
        return false;
      }
    }
    // all checks passed
    return true;
  }
}
