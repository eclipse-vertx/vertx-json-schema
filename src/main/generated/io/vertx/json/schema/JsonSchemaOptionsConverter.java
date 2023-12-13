package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.json.schema.JsonSchemaOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.json.schema.JsonSchemaOptions} original class using Vert.x codegen.
 */
public class JsonSchemaOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, JsonSchemaOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "baseUri":
          if (member.getValue() instanceof String) {
            obj.setBaseUri((String)member.getValue());
          }
          break;
        case "draft":
          if (member.getValue() instanceof String) {
            obj.setDraft(io.vertx.json.schema.Draft.valueOf((String)member.getValue()));
          }
          break;
        case "outputFormat":
          if (member.getValue() instanceof String) {
            obj.setOutputFormat(io.vertx.json.schema.OutputFormat.valueOf((String)member.getValue()));
          }
          break;
      }
    }
  }

   static void toJson(JsonSchemaOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(JsonSchemaOptions obj, java.util.Map<String, Object> json) {
    if (obj.getBaseUri() != null) {
      json.put("baseUri", obj.getBaseUri());
    }
    if (obj.getDraft() != null) {
      json.put("draft", obj.getDraft().name());
    }
    if (obj.getOutputFormat() != null) {
      json.put("outputFormat", obj.getOutputFormat().name());
    }
  }
}
