package io.vertx.json.schema.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import io.vertx.core.json.JsonArray;

import java.io.StringWriter;
import java.util.Objects;

public class JsonArrayProxy extends JsonArray implements JsonProxyEncoder {

  public JsonArrayProxy() {
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

  public boolean equals(Object o) {
    if (o == null) {
      return false;
    } else if (this == o) {
      return true;
    } else if (!(o instanceof JsonArray)) {// This is important otherwise our tests will fail.
      return false;
    } else {
      JsonArray other = (JsonArray) o;
      if (this.size() != other.size()) {
        return false;
      } else {
        for (int i = 0; i < this.size(); ++i) {
          Object thisValue = this.getValue(i);
          Object otherValue = other.getValue(i);
          if (thisValue != otherValue) {
            if (thisValue instanceof Number && otherValue instanceof Number && thisValue.getClass() != otherValue.getClass()) {
              Number n1 = (Number) thisValue;
              Number n2 = (Number) otherValue;
              if ((thisValue instanceof Float || thisValue instanceof Double || otherValue instanceof Float || otherValue instanceof Double) && n1.doubleValue() == n2.doubleValue() || (thisValue instanceof Integer || thisValue instanceof Long || otherValue instanceof Integer || otherValue instanceof Long) && n1.longValue() == n2.longValue()) {
                continue;
              }
            }

            if (thisValue instanceof CharSequence && otherValue instanceof CharSequence && thisValue.getClass() != otherValue.getClass()) {
              CharSequence s1 = (CharSequence) thisValue;
              CharSequence s2 = (CharSequence) otherValue;
              if (Objects.equals(s1.toString(), s2.toString())) {
                continue;
              }
            }

            if (!Objects.equals(thisValue, otherValue)) {
              return false;
            }
          }
        }

        return true;
      }
    }
  }
}
