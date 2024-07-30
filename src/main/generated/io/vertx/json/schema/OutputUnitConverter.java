package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.json.schema.OutputUnit}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.json.schema.OutputUnit} original class using Vert.x codegen.
 */
public class OutputUnitConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, OutputUnit obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "valid":
          if (member.getValue() instanceof Boolean) {
            obj.setValid((Boolean)member.getValue());
          }
          break;
        case "absoluteKeywordLocation":
          if (member.getValue() instanceof String) {
            obj.setAbsoluteKeywordLocation((String)member.getValue());
          }
          break;
        case "keywordLocation":
          if (member.getValue() instanceof String) {
            obj.setKeywordLocation((String)member.getValue());
          }
          break;
        case "instanceLocation":
          if (member.getValue() instanceof String) {
            obj.setInstanceLocation((String)member.getValue());
          }
          break;
        case "error":
          if (member.getValue() instanceof String) {
            obj.setError((String)member.getValue());
          }
          break;
        case "errors":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.json.schema.OutputUnit> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.json.schema.OutputUnit((io.vertx.core.json.JsonObject)item));
            });
            obj.setErrors(list);
          }
          break;
        case "annotations":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.json.schema.OutputUnit> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.json.schema.OutputUnit((io.vertx.core.json.JsonObject)item));
            });
            obj.setAnnotations(list);
          }
          break;
        case "errorType":
          if (member.getValue() instanceof String) {
            obj.setErrorType(io.vertx.json.schema.OutputErrorType.valueOf((String)member.getValue()));
          }
          break;
      }
    }
  }

   static void toJson(OutputUnit obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(OutputUnit obj, java.util.Map<String, Object> json) {
    if (obj.getValid() != null) {
      json.put("valid", obj.getValid());
    }
    if (obj.getAbsoluteKeywordLocation() != null) {
      json.put("absoluteKeywordLocation", obj.getAbsoluteKeywordLocation());
    }
    if (obj.getKeywordLocation() != null) {
      json.put("keywordLocation", obj.getKeywordLocation());
    }
    if (obj.getInstanceLocation() != null) {
      json.put("instanceLocation", obj.getInstanceLocation());
    }
    if (obj.getError() != null) {
      json.put("error", obj.getError());
    }
    if (obj.getErrors() != null) {
      JsonArray array = new JsonArray();
      obj.getErrors().forEach(item -> array.add(item.toJson()));
      json.put("errors", array);
    }
    if (obj.getAnnotations() != null) {
      JsonArray array = new JsonArray();
      obj.getAnnotations().forEach(item -> array.add(item.toJson()));
      json.put("annotations", array);
    }
    if (obj.getErrorType() != null) {
      json.put("errorType", obj.getErrorType().name());
    }
  }
}
