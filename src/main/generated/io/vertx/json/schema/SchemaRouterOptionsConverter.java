package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.json.schema.SchemaRouterOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.json.schema.SchemaRouterOptions} original class using Vert.x codegen.
 */
public class SchemaRouterOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, SchemaRouterOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "authHeaders":
          if (member.getValue() instanceof JsonObject) {
          }
          break;
        case "authQueryParams":
          if (member.getValue() instanceof JsonObject) {
          }
          break;
      }
    }
  }

  public static void toJson(SchemaRouterOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(SchemaRouterOptions obj, java.util.Map<String, Object> json) {
    if (obj.getAuthHeaders() != null) {
      JsonObject map = new JsonObject();
      obj.getAuthHeaders().forEach((key, value) -> map.put(key, value));
      json.put("authHeaders", map);
    }
    if (obj.getAuthQueryParams() != null) {
      JsonObject map = new JsonObject();
      obj.getAuthQueryParams().forEach((key, value) -> map.put(key, value));
      json.put("authQueryParams", map);
    }
  }
}
