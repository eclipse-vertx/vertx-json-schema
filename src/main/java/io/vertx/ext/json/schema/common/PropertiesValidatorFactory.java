package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class PropertiesValidatorFactory implements ValidatorFactory {

  private Schema parseAdditionalProperties(Object obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      return parser.parse(obj, scope.copy().append("additionalProperties"), parent);
    } catch (ClassCastException e) {
      throw new SchemaException(obj, "Wrong type for additionalProperties keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(obj, "Null additionalProperties keyword", e);
    }
  }

  private Map<String, Schema> parseProperties(JsonObject obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    JsonPointer basePointer = scope.copy().append("properties");
    Map<String, Schema> parsedSchemas = new HashMap<>();
    for (Map.Entry<String, Object> entry : obj) {
      try {
        parsedSchemas.put(entry.getKey(), parser.parse(
            entry.getValue(),
            basePointer.copy().append(entry.getKey()),
            parent
        ));
      } catch (ClassCastException | NullPointerException e) {
        throw new SchemaException(obj, "Property descriptor " + entry.getKey() + " should be a not null JsonObject", e);
      }
    }
    return parsedSchemas;
  }

  private Map<Pattern, Schema> parsePatternProperties(JsonObject obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    JsonPointer basePointer = scope.copy().append("patternProperties");
    Map<Pattern, Schema> parsedSchemas = new HashMap<>();
    for (Map.Entry<String, Object> entry : obj) {
      try {
        parsedSchemas.put(Pattern.compile(entry.getKey()), parser.parse(
            entry.getValue(),
            basePointer.copy().append(entry.getKey()),
            parent
        ));
      } catch (PatternSyntaxException e) {
        throw new SchemaException(obj, "Invalid pattern for pattern keyword", e);
      } catch (ClassCastException | NullPointerException e) {
        throw new SchemaException(obj, "Property descriptor " + entry.getKey() + " should be a not null JsonObject", e);
      }
    }
    return parsedSchemas;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject properties = schema.getJsonObject("properties");
      JsonObject patternProperties = schema.getJsonObject("patternProperties");
      Object additionalProperties = schema.getValue("additionalProperties");

      PropertiesValidator validator = new PropertiesValidator(parent);

      Map<String, Schema> parsedProperties = (properties != null) ? parseProperties(properties, scope, parser, validator) : null;
      Map<Pattern, Schema> parsedPatternProperties = (patternProperties != null) ? parsePatternProperties(patternProperties, scope, parser, validator) : null;

      if (additionalProperties instanceof JsonObject) {
        validator.configure(parsedProperties, parsedPatternProperties, parseAdditionalProperties(additionalProperties, scope, parser, validator));
      } else if (additionalProperties instanceof Boolean) {
        validator.configure(parsedProperties, parsedPatternProperties, (Boolean) additionalProperties);
      } else {
        validator.configure(parsedProperties, parsedPatternProperties, true);
      }
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for properties/patternProperties keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("properties") || schema.containsKey("patternProperties") || schema.containsKey("additionalProperties");
  }

  private Future<Void> fillAdditionalPropertyException(Throwable t, Object in) {
    return Future.failedFuture(createException("additionalProperties schema should match", "additionalProperties", in, t));
  }

  class PropertiesValidator extends BaseMutableStateValidator implements ValidatorWithDefaultApply {

    private Map<String, Schema> properties;
    private Map<Pattern, Schema> patternProperties;
    private boolean allowAdditionalProperties;
    private Schema additionalPropertiesSchema;

    public PropertiesValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(Map<String, Schema> properties, Map<Pattern, Schema> patternProperties, boolean allowAdditionalProperties) {
      this.properties = properties;
      this.patternProperties = patternProperties;
      this.allowAdditionalProperties = allowAdditionalProperties;
      this.additionalPropertiesSchema = null;
      initializeIsSync();
    }

    private void configure(Map<String, Schema> properties, Map<Pattern, Schema> patternProperties, Schema additionalPropertiesSchema) {
      this.properties = properties;
      this.patternProperties = patternProperties;
      this.allowAdditionalProperties = true;
      this.additionalPropertiesSchema = additionalPropertiesSchema;
      initializeIsSync();
    }

    @Override
    public boolean calculateIsSync() {
      Stream<Boolean> props = (properties != null) ? properties.values().stream().map(Schema::isSync) : Stream.empty();
      Stream<Boolean> patternProps = (patternProperties != null) ? patternProperties.values().stream().map(Schema::isSync) : Stream.empty();
      Stream<Boolean> additionalProps = (additionalPropertiesSchema != null) ? Stream.of(additionalPropertiesSchema.isSync()) : Stream.empty();
      return Stream.concat(
          props,
          Stream.concat(patternProps, additionalProps)
      ).reduce(true, Boolean::logicalAnd);
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        List<Future> futs = new ArrayList<>();
        for (String key : obj.fieldNames()) {
          boolean found = false;
          if (properties != null && properties.containsKey(key)) {
            Schema s = properties.get(key);
            if (s.isSync()) {
              try {
                s.validateSync(obj.getValue(key));
              } catch (ValidationException e) {
                return Future.failedFuture(e);
              }
            } else {
              futs.add(s.validateAsync(obj.getValue(key)));
            }
            found = true;
          }
          if (patternProperties != null) {
            for (Map.Entry<Pattern, Schema> patternProperty : patternProperties.entrySet()) {
              if (patternProperty.getKey().matcher(key).find()) {
                Schema s = patternProperty.getValue();
                if (s.isSync()) {
                  try {
                    s.validateSync(obj.getValue(key));
                  } catch (ValidationException e) {
                    return Future.failedFuture(e);
                  }
                } else {
                  futs.add(s.validateAsync(obj.getValue(key)));
                }
                found = true;
              }
            }
          }
          if (!found) {
            if (allowAdditionalProperties) {
              if (additionalPropertiesSchema != null) {
                if (additionalPropertiesSchema.isSync()) {
                  try {
                    additionalPropertiesSchema.validateSync(obj.getValue(key));
                  } catch (ValidationException e) {
                    return fillAdditionalPropertyException(e, in);
                  }
                } else {
                  futs.add(additionalPropertiesSchema.validateAsync(obj.getValue(key)).recover(t -> fillAdditionalPropertyException(t, in)));
                }
              }
            } else {
              return Future.failedFuture(createException("provided object should not contain additional properties", "additionalProperties", in));
            }
          }
        }
        if (futs.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futs).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      this.checkSync();
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        for (String key : obj.fieldNames()) {
          boolean found = false;
          if (properties != null && properties.containsKey(key)) {
            Schema s = properties.get(key);
            s.validateSync(obj.getValue(key));
            found = true;
          }
          if (patternProperties != null) {
            for (Map.Entry<Pattern, Schema> patternProperty : patternProperties.entrySet()) {
              if (patternProperty.getKey().matcher(key).find()) {
                Schema s = patternProperty.getValue();
                s.validateSync(obj.getValue(key));
                found = true;
              }
            }
          }
          if (!found) {
            if (allowAdditionalProperties) {
              if (additionalPropertiesSchema != null) {
                additionalPropertiesSchema.validateSync(obj.getValue(key));
              }
            } else {
              throw createException("provided object should not contain additional properties", "additionalProperties", in);
            }
          }
        }
      }
    }

    @Override
    public void applyDefaultValue(Object value) {
      if (value instanceof JsonObject && properties != null) {
        JsonObject obj = (JsonObject) value;
        for (Map.Entry<String, Schema> e : properties.entrySet()) {
          if (!obj.containsKey(e.getKey()) && e.getValue().hasDefaultValue()) {
            obj.put(e.getKey(), e.getValue().getDefaultValue());
          } else if (obj.containsKey(e.getKey())) {
            ((SchemaImpl)e.getValue()).doApplyDefaultValues(obj.getValue(e.getKey()));
          }
        }
      }
    }
  }

}
