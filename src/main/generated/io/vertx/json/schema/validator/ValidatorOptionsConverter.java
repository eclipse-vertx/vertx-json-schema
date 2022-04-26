package io.vertx.json.schema.validator;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.json.schema.validator.ValidatorOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.json.schema.validator.ValidatorOptions} original class using Vert.x codegen.
 */
public class ValidatorOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ValidatorOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "baseUri":
          if (member.getValue() instanceof String) {
            obj.setBaseUri((String)member.getValue());
          }
          break;
        case "draft":
          if (member.getValue() instanceof String) {
            obj.setDraft(io.vertx.json.schema.validator.Draft.valueOf((String)member.getValue()));
          }
          break;
        case "outputFormat":
          if (member.getValue() instanceof String) {
            obj.setOutputFormat(io.vertx.json.schema.validator.OutputFormat.valueOf((String)member.getValue()));
          }
          break;
      }
    }
  }

  public static void toJson(ValidatorOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(ValidatorOptions obj, java.util.Map<String, Object> json) {
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
