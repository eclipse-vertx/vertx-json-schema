package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class JsonObjectProxy extends JsonObject {

  public JsonObjectProxy(JsonObject json) {
    super(json.getMap());
  }

  @Override
  public String encode() {
    StringBuilder sb = new StringBuilder();
    encode(this, sb);
    return sb.toString();
  }

  private static void encode(Object val, StringBuilder sb) {
    if (val instanceof JsonObjectRef) {
      JsonObjectRef ref = (JsonObjectRef) val;
      String s = ref.getString("__absolute_uri__");
      sb.append("{\"$ref\":\"").append(s).append("\"}");
      return;
    }
    if (val instanceof JsonObject) {
      JsonObject obj = (JsonObject) val;
      sb.append('{');
      for (Map.Entry<String, Object> entry : obj) {
        if (entry.getKey().equals("__absolute_uri__")) {
          continue;
        }
        sb.append('"');
        sb.append(entry.getKey());
        sb.append("\":");
        encode(entry.getValue(), sb);
      }
      sb.append('}');
    } else if (val instanceof JsonArray) {
      sb.append('[');
      JsonArray arr = (JsonArray) val;
      boolean first = true;
      for (Object entry : arr) {
        if (first) {
          first = false;
        } else {
          sb.append(',');
        }
        encode(entry, sb);
      }
      sb.append(']');
    } else if (val instanceof String) {
      sb.append('"');
      // NOT GOOD SHOULD ESCAPE THINGS
      sb.append(val);
      sb.append('"');
    } else {
      // NOT GOOD BUT THAT WORKS WELL ENOUGH
      sb.append(val);
    }
  }
}
