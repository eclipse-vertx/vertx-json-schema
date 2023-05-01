/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static io.vertx.json.schema.ValidationException.create;
import static io.vertx.json.schema.common.JsonUtil.unwrap;

public class PropertiesValidatorFactory implements ValidatorFactory {

  private SchemaInternal parseAdditionalProperties(Object obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      return parser.parse(obj, scope.copy().append("additionalProperties"), parent);
    } catch (ClassCastException e) {
      throw new SchemaException(obj, "Wrong type for additionalProperties keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(obj, "Null additionalProperties keyword", e);
    }
  }

  private Map<String, SchemaInternal> parseProperties(JsonObject obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    JsonPointer basePointer = scope.copy().append("properties");
    Map<String, SchemaInternal> parsedSchemas = new HashMap<>();
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

  private Map<Pattern, SchemaInternal> parsePatternProperties(JsonObject obj, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    JsonPointer basePointer = scope.copy().append("patternProperties");
    Map<Pattern, SchemaInternal> parsedSchemas = new HashMap<>();
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

      Map<String, SchemaInternal> parsedProperties = (properties != null) ? parseProperties(properties, scope, parser, validator) : null;
      Map<Pattern, SchemaInternal> parsedPatternProperties = (patternProperties != null) ? parsePatternProperties(patternProperties, scope, parser, validator) : null;

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
    return Future.failedFuture(ValidationException.create("additionalProperties schema should match", "additionalProperties", in, t));
  }

  class PropertiesValidator extends BaseMutableStateValidator implements DefaultApplier {

    private Map<String, SchemaInternal> properties;
    private Map<Pattern, SchemaInternal> patternProperties;
    private boolean allowAdditionalProperties;
    private SchemaInternal additionalPropertiesSchema;

    public PropertiesValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(Map<String, SchemaInternal> properties, Map<Pattern, SchemaInternal> patternProperties, boolean allowAdditionalProperties) {
      this.properties = properties;
      this.patternProperties = patternProperties;
      this.allowAdditionalProperties = allowAdditionalProperties;
      this.additionalPropertiesSchema = null;
      initializeIsSync();
    }

    private void configure(Map<String, SchemaInternal> properties, Map<Pattern, SchemaInternal> patternProperties, SchemaInternal additionalPropertiesSchema) {
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
    public Future<Void> validateAsync(ValidatorContext context, final Object in) throws ValidationException {
      if (isSync()) return validateSyncAsAsync(context, in);
      Object o = unwrap(in);
      if (o instanceof Map<?, ?>) {
        Map<String, ?> obj = (Map<String, ?>) o;
        List<Future<Void>> futs = new ArrayList<>();
        for (String key : obj.keySet()) {
          boolean found = false;
          if (properties != null && properties.containsKey(key)) {
            SchemaInternal s = properties.get(key);
            context.markEvaluatedProperty(key);
            if (s.isSync()) {
              try {
                s.validateSync(context.lowerLevelContext(key), obj.get(key));
              } catch (ValidationException e) {
                return Future.failedFuture(e);
              }
            } else {
              futs.add(s.validateAsync(context.lowerLevelContext(key), obj.get(key)));
            }
            found = true;
          }
          if (patternProperties != null) {
            for (Map.Entry<Pattern, SchemaInternal> patternProperty : patternProperties.entrySet()) {
              if (patternProperty.getKey().matcher(key).find()) {
                SchemaInternal s = patternProperty.getValue();
                context.markEvaluatedProperty(key);
                if (s.isSync()) {
                  try {
                    s.validateSync(context.lowerLevelContext(key), obj.get(key));
                  } catch (ValidationException e) {
                    return Future.failedFuture(e);
                  }
                } else {
                  futs.add(s.validateAsync(context.lowerLevelContext(key), obj.get(key)));
                }
                found = true;
              }
            }
          }
          if (!found) {
            if (allowAdditionalProperties) {
              if (additionalPropertiesSchema != null) {
                context.markEvaluatedProperty(key);
                if (additionalPropertiesSchema.isSync()) {
                  try {
                    additionalPropertiesSchema.validateSync(context.lowerLevelContext(key), obj.get(key));
                  } catch (ValidationException e) {
                    return fillAdditionalPropertyException(e, in);
                  }
                } else {
                  futs.add(additionalPropertiesSchema
                    .validateAsync(context.lowerLevelContext(key), obj.get(key))
                    .recover(t -> fillAdditionalPropertyException(t, in))
                  );
                }
              }
            } else {
              return Future.failedFuture(create("Provided object contains unexpected additional property: " + key, "additionalProperties", in));
            }
          }
        }
        if (futs.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futs).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, final Object in) throws ValidationException {
      this.checkSync();
      Object o = unwrap(in);
      if (o instanceof Map<?, ?>) {
        Map<String, ?> obj = (Map<String, ?>) o;
        for (String key : obj.keySet()) {
          boolean found = false;
          if (properties != null && properties.containsKey(key)) {
            SchemaInternal s = properties.get(key);
            context.markEvaluatedProperty(key);
            s.validateSync(context.lowerLevelContext(key), obj.get(key));
            found = true;
          }
          if (patternProperties != null) {
            for (Map.Entry<Pattern, SchemaInternal> patternProperty : patternProperties.entrySet()) {
              if (patternProperty.getKey().matcher(key).find()) {
                SchemaInternal s = patternProperty.getValue();
                context.markEvaluatedProperty(key);
                s.validateSync(context.lowerLevelContext(key), obj.get(key));
                found = true;
              }
            }
          }
          if (!found) {
            if (allowAdditionalProperties) {
              if (additionalPropertiesSchema != null) {
                context.markEvaluatedProperty(key);
                additionalPropertiesSchema.validateSync(context.lowerLevelContext(key), obj.get(key));
              }
            } else {
              throw create("Provided object contains unexpected additional property: " + key, "additionalProperties", in);
            }
          }
        }
      }
    }

    @Override
    public Future<Void> applyDefaultValue(Object value) {
      value = unwrap(value);
      if (!(value instanceof Map<?, ?> && properties != null)) {
        return Future.succeededFuture();
      }

      List<Future<?>> futs = new ArrayList<>();
      Map<String, Object> obj = (Map<String, Object>) value;
      for (Map.Entry<String, SchemaInternal> e : properties.entrySet()) {
        final String key = e.getKey();
        final SchemaInternal schema = e.getValue();

        if (!obj.containsKey(key)) {
          if (schema.isSync()) {
            Object def = schema.getOrApplyDefaultSync(null);
            if (def != null) {
              obj.put(key, def);
            }
          } else {
            futs.add(
              schema.getOrApplyDefaultAsync(null).onSuccess(def -> {
                if (def != null) {
                  obj.put(key, def);
                }
              })
            );
          }
        } else {
          if (schema.isSync()) {
            schema.getOrApplyDefaultSync(obj.get(key));
          } else {
            futs.add(
              schema.getOrApplyDefaultAsync(obj.get(key))
            );
          }
        }
      }

      if (futs.isEmpty()) {
        return Future.succeededFuture();
      }

      return CompositeFuture.all(futs).mapEmpty();
    }
  }

}
