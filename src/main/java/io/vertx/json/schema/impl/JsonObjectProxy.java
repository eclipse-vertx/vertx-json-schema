package io.vertx.json.schema.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.vertx.core.json.impl.JsonUtil.BASE64_ENCODER;
import static io.vertx.json.schema.JsonSchema.EXCLUDE_ANNOTATION_ENTRIES;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toMap;

public class JsonObjectProxy extends JsonObject {
  private static final JsonFactory factory = new JsonFactory();

  public JsonObjectProxy(JsonObject json) {
    super(json.getMap());
  }

  private static final String KEY_ABS_URI = "__absolute_uri__";

  static {
    factory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
  }

  private static JsonGenerator createGenerator(Writer out, boolean pretty) {
    try {
      JsonGenerator generator = factory.createGenerator(out);
      if (pretty) {
        generator.useDefaultPrettyPrinter();
      }

      return generator;
    } catch (IOException e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

  @Override
  public JsonObject getJsonObject(String key) {
    JsonObject o = super.getJsonObject(key);
    return o == null ? null : new JsonObjectProxy(o);
  }

  @Override
  public JsonObject getJsonObject(String key, JsonObject def) {
    JsonObject o = super.getJsonObject(key, def);
    return o == null ? null : new JsonObjectProxy(o);
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

  private String encode(StringWriter sw, JsonGenerator generator) {
    encodeJson(this, generator);
    try {
      generator.flush();
      return sw.toString();
    } catch (IOException e) {
      throw new EncodeException(e.getMessage(), e);
    }
  }


  private static void encodeJson(Object json, JsonGenerator generator) throws EncodeException {
    try {
      if (json instanceof JsonObjectRef) {
        JsonObjectRef ref = (JsonObjectRef) json;
        String s = ref.getString(KEY_ABS_URI);
        if (s != null) {
          generator.writeStartObject();
          generator.writeStringField("$ref", s);
          generator.writeEndObject();
          return;
        }
      }
      if (json instanceof JsonObject) {
        Map<String, Object> properties = ((JsonObject) json).getMap();
        json = properties.entrySet().stream()
          .filter(EXCLUDE_ANNOTATION_ENTRIES)
          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
      } else if (json instanceof JsonArray) {
        json = ((JsonArray) json).getList();
      }
      if (json instanceof Map) {
        generator.writeStartObject();
        for (Map.Entry<String, ?> e : ((Map<String, ?>) json).entrySet()) {
          generator.writeFieldName(e.getKey());
          encodeJson(e.getValue(), generator);
        }
        generator.writeEndObject();
      } else if (json instanceof List) {
        generator.writeStartArray();
        for (Object item : (List<?>) json) {
          encodeJson(item, generator);
        }
        generator.writeEndArray();
      } else if (json instanceof String) {
        generator.writeString((String) json);
      } else if (json instanceof Number) {
        if (json instanceof Short) {
          generator.writeNumber((Short) json);
        } else if (json instanceof Integer) {
          generator.writeNumber((Integer) json);
        } else if (json instanceof Long) {
          generator.writeNumber((Long) json);
        } else if (json instanceof Float) {
          generator.writeNumber((Float) json);
        } else if (json instanceof Double) {
          generator.writeNumber((Double) json);
        } else if (json instanceof Byte) {
          generator.writeNumber((Byte) json);
        } else if (json instanceof BigInteger) {
          generator.writeNumber((BigInteger) json);
        } else if (json instanceof BigDecimal) {
          generator.writeNumber((BigDecimal) json);
        } else {
          generator.writeNumber(((Number) json).doubleValue());
        }
      } else if (json instanceof Boolean) {
        generator.writeBoolean((Boolean) json);
      } else if (json instanceof Instant) {
        // RFC-7493
        generator.writeString((ISO_INSTANT.format((Instant) json)));
      } else if (json instanceof byte[]) {
        // RFC-7493
        generator.writeString(BASE64_ENCODER.encodeToString((byte[]) json));
      } else if (json instanceof Buffer) {
        // RFC-7493
        generator.writeString(BASE64_ENCODER.encodeToString(((Buffer) json).getBytes()));
      } else if (json instanceof Enum) {
        // vert.x extra (non standard but allowed conversion)
        generator.writeString(((Enum<?>) json).name());
      } else if (json == null) {
        generator.writeNull();
      } else {
        throw new EncodeException("Mapping " + json.getClass().getName() + "  is not available without Jackson " +
          "Databind on the classpath");
      }
    } catch (IOException e) {
      throw new EncodeException(e.getMessage(), e);
    }
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
