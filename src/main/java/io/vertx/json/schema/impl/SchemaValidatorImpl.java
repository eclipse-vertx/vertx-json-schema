package io.vertx.json.schema.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;

import java.util.Objects;
import java.util.*;
import java.util.regex.Pattern;

import static io.vertx.json.schema.impl.SchemaRepositoryImpl.dereference;
import static io.vertx.json.schema.impl.Utils.*;

public class SchemaValidatorImpl implements SchemaValidatorInternal {

  private final Map<String, JsonSchema> lookup;

  private final JsonSchema schema;
  private final Draft draft;
  private final OutputFormat outputFormat;
  private final JsonFormatValidator formatValidator;

  public SchemaValidatorImpl(JsonSchema schema, JsonSchemaOptions options, Map<String, JsonSchema> lookup,
                             boolean dereference, JsonFormatValidator formatValidator) {
    Objects.requireNonNull(schema, "'schema' cannot be null");
    Objects.requireNonNull(options, "'options' cannot be null");
    Objects.requireNonNull(options.getOutputFormat(), "'options.outputFormat' cannot be null");
    Objects.requireNonNull(lookup, "'lookup' cannot be null");
    Objects.requireNonNull(formatValidator, "'formatValidator' cannot be null");

    this.formatValidator = formatValidator;
    this.schema = schema;
    // extract the draft from schema when no specific draft is configured in the options
    this.draft = options.getDraft() == null ?
      Draft.fromIdentifier(schema.get("$schema")) :
      options.getDraft();
    this.outputFormat = options.getOutputFormat();
    this.lookup = new HashMap<>(lookup);
    if (dereference) {
      URL baseUri = new URL(options.getBaseUri());
      // add the root schema
      dereference(this.lookup, schema, baseUri, "", true);
    }
  }

  @Override
  public JsonSchema schema() {
    return schema;
  }

  @Override
  public OutputUnit validate(Object instance) throws SchemaException {
    return validate(
      instance,
      schema,
      null,
      "#",
      "#",
      "#",
      new HashSet<>(),
      new HashMap<>());
  }

  /**
   * Validate an instance against a schema.
   *
   * @param _instance        this is the object instance to validate
   * @param schema           this is the current schema, it will change as we recurse the schema and do recursive calls with the
   *                         sub-schema
   * @param _recursiveAnchor when dealing with recursive anchors (pre 2020-12 draft) this is the schema that is the
   *                         anchor
   * @param instanceLocation tracks the instance location (needed to build the correct error messages)
   * @param schemaLocation   tracks the schema location (needed to build the correct error messages)
   * @param baseLocation     tracks the location from start to current schema (needed to build the correct error messages)
   * @param evaluated        tracks evaluated schemas needed for or/oneOf/anyOf/all/etc... like validations
   * @param dynamicContext   tracks the dynamic context needed for $dynamicRef (2020-12 draft). Keys start with # for
   *                         dynamic anchors, $ are used for 2019-09 fragments
   * @return the validation result
   * @throws SchemaException when the schema is not resolvable (unknown $ref)
   */
  private OutputUnit validate(final Object _instance, final JsonSchema schema, final JsonSchema _recursiveAnchor, final String instanceLocation, final String schemaLocation, final String baseLocation, final Set<Object> evaluated, final Map<String, Deque<JsonSchema>> dynamicContext) throws SchemaException {

    // the are 2 kinds of schemas BooleanSchema and JsonSchema
    // Boolean schemas are terminal and require no further processing.

    // All schemas will be composed to end with a terminal schema.
    if (schema instanceof BooleanSchema) {
      if (schema == BooleanSchema.TRUE) {
        return new OutputUnit(true);
      } else {
        return new OutputUnit(false).setErrorType(OutputErrorType.INVALID_VALUE);
      }
    }

    // adapt JSON types. This is needed because the JSON types are not the same as Java types, and this ensures that we
    // always work on the regular type space (JSON types).
    final Object instance = JSON.jsonify(_instance);

    // start validating
    String instanceType = JSON.typeOf(instance);
    List<OutputUnit> errors = new ArrayList<>();
    List<OutputUnit> annotations = new ArrayList<>();

    final String dynamicAnchor;

    // push $dynamicAnchor with current "__absolute_uri__"
    if (schema.containsKey("$dynamicAnchor")) {
      dynamicAnchor = "#" + schema.get("$dynamicAnchor");
      dynamicContext
        .computeIfAbsent(dynamicAnchor, k -> new LinkedList<>())
        .add(schema);
    } else {
      dynamicAnchor = null;
    }

    // Lock (recursive anchor to the current schema, is dealing with $recursiveAnchor)
    final JsonSchema recursiveAnchor;
    if (_recursiveAnchor == null && schema.<Boolean>get("$recursiveAnchor", false)) {
      recursiveAnchor = schema;
    } else {
      recursiveAnchor = _recursiveAnchor;
    }

    if ("#".equals(schema.get("$recursiveRef"))) {
      assert schema.containsKey("__absolute_recursive_ref__");
      final JsonSchema refSchema =
        recursiveAnchor == null
          ? lookup.get(schema.<String>get("__absolute_recursive_ref__"))
          : recursiveAnchor;
      final OutputUnit result = validate(
        instance,
        recursiveAnchor == null ? schema : recursiveAnchor,
        refSchema,
        instanceLocation,
        schemaLocation + "/$recursiveRef",
        baseLocation + "/$recursiveRef",
        evaluated,
        dynamicContext
      );
      if (!result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/$recursiveRef"), baseLocation + "/$recursiveRef", "A sub-schema had errors", result.getErrorType()));
        if (result.getErrors() != null) {
          errors.addAll(result.getErrors());
        }
      }
    }

    if (schema.containsKey("$dynamicRef")) {
      Deque<JsonSchema> deque = dynamicContext.get(schema.<String>get("$dynamicRef"));
      if (deque != null) {
        JsonSchema head = deque.peekFirst();
        if (head != null) {
          // compute the dynamic reference uri
          String uri = new URL(schema.<String>get("$dynamicRef"), head.<String>get("__absolute_uri__")).href();

          if (!lookup.containsKey(uri)) {
            String message = "Unresolved $dynamicRef " + schema.<String>get("$dynamicRef");
            message += "\nKnown schemas:\n- " + String.join("\n- ", lookup.keySet());
            throw new SchemaException(schema, message);
          }

          final JsonSchema refSchema = lookup.get(uri);
          final OutputUnit result = validate(
            instance,
            recursiveAnchor == null ? schema : recursiveAnchor,
            refSchema,
            instanceLocation,
            schemaLocation + "/$dynamicRef",
            baseLocation + "/$dynamicRef",
            evaluated,
            dynamicContext
          );
          if (!result.getValid()) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/$dynamicRef"), baseLocation + "/$dynamicRef", "A sub-schema had errors", result.getErrorType()));
            if (result.getErrors() != null) {
              errors.addAll(result.getErrors());
            }
          }
          if (draft == Draft.DRAFT4 || draft == Draft.DRAFT7) {
            if (dynamicAnchor != null) {
              dynamicContext
                .get(dynamicAnchor)
                .removeLast();
            }
            return new OutputUnit(errors.isEmpty()).setErrors(errors).setErrorType(errors.isEmpty() ? OutputErrorType.NONE : errors.get(0).getErrorType());
          }
        }
      }
    }

    if (schema.containsKey("$ref")) {
      final String uri = schema.get("__absolute_ref__", schema.get("$ref"));
      if (!lookup.containsKey(uri)) {
        String message = "Unresolved $ref " + schema.<String>get("$ref");
        if (schema.containsKey("__absolute_ref__") && !schema.get("__absolute_ref__").equals(schema.<String>get("$ref"))) {
          message += ": Absolute URI " + schema.get("__absolute_ref__");
        }
        message += "\nKnown schemas:\n- " + String.join("\n- ", lookup.keySet());
        throw new SchemaException(schema, message);
      }

      final JsonSchema refSchema = lookup.get(uri);
      final OutputUnit result = validate(
        instance,
        refSchema,
        recursiveAnchor,
        instanceLocation,
        schema.get("$ref"),
        baseLocation + "/$ref",
        evaluated,
        dynamicContext
      );
      if (!result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/$ref"), baseLocation + "/$ref", "A subschema had errors", result.getErrorType()));
        if (result.getErrors() != null) {
          errors.addAll(result.getErrors());
        }
      }
      if (draft == Draft.DRAFT4 || draft == Draft.DRAFT7) {
        return new OutputUnit(errors.isEmpty())
          .setErrors(outputFormat == OutputFormat.Flag ? null : errors.isEmpty() ? null : errors)
          .setErrorType(outputFormat == OutputFormat.Flag ? null : errors.isEmpty() ? OutputErrorType.NONE : errors.get(0).getErrorType());
      }
    }

    if (schema.get("type") instanceof JsonArray) {
      final JsonArray type = schema.get("type");
      int length = type.size();
      boolean valid = false;
      for (int i = 0; i < length; i++) {
        if (
          instanceType.equals(type.getString(i)) ||
            ("integer".equals(type.getString(i)) && "number".equals(instanceType) && Numbers.isInteger(instance))) {
          valid = true;
          break;
        }
      }
      if (!valid) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/type"), baseLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + String.join(", ", type.getList()), OutputErrorType.INVALID_VALUE));
      }
    } else if ("integer".equals(schema.get("type"))) {
      if (!"number".equals(instanceType) || !Numbers.isInteger(instance)) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/type"), baseLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type"), OutputErrorType.INVALID_VALUE));
      }
    } else if (schema.containsKey("type") && !instanceType.equals(schema.get("type"))) {
      errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/type"), baseLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type"), OutputErrorType.INVALID_VALUE));
    }

    if (schema.containsKey("const")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (!JSON.deepCompare(instance, schema.get("const"))) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/const"), baseLocation + "/const", "Instance does not match " + Json.encode(schema.get("const")), OutputErrorType.INVALID_VALUE));
        }
      } else if (!Utils.Objects.equals(schema.get("const"), instance)) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/const"), baseLocation + "/const", "Instance does not match " + Json.encode(schema.get("const")), OutputErrorType.INVALID_VALUE));
      }
    }

    if (schema.containsKey("enum")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (schema.<JsonArray>get("enum").stream().noneMatch(value -> JSON.deepCompare(instance, value))) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/enum"), baseLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum")), OutputErrorType.INVALID_VALUE));
        }
      } else if (schema.<JsonArray>get("enum").stream().noneMatch(value -> Utils.Objects.equals(instance, value))) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/enum"), baseLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum")), OutputErrorType.INVALID_VALUE));
      }
    }

    if (schema.containsKey("not")) {
      final OutputUnit result = validate(
        instance,
        Schemas.wrap((JsonObject) schema, "not"),
        recursiveAnchor,
        instanceLocation,
        schemaLocation + "/not",
        baseLocation + "/not",
        new HashSet<>(),
        dynamicContext
      );
      if (result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/not"), baseLocation + "/not", "Instance matched \"not\" schema", result.getErrorType()));
      }
    }

    Set<Object> subEvaluateds = new HashSet<>();

    if (schema.containsKey("anyOf")) {
      final int errorsLength = errors.size();
      boolean anyValid = false;
      for (int i = 0; i < schema.<JsonArray>get("anyOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("anyOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          schemaLocation + "/anyOf/" + i,
          baseLocation + "/anyOf/" + i,
          subEvaluated,
          dynamicContext
        );
        if (result.getErrors() != null) {
          errors.addAll(result.getErrors());
        }
        anyValid = anyValid || result.getValid();
        if (result.getValid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (anyValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/anyOf"), baseLocation + "/anyOf", "Instance does not match any subschemas", OutputErrorType.INVALID_VALUE));
      }
    }

    if (schema.containsKey("allOf")) {
      final int errorsLength = errors.size();
      boolean allValid = true;
      for (int i = 0; i < schema.<JsonArray>get("allOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("allOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          schemaLocation + "/allOf/" + i,
          baseLocation + "/allOf/" + i,
          subEvaluated,
          dynamicContext
        );
        if (result.getErrors() != null) {
          errors.addAll(result.getErrors());
        }
        allValid = allValid && result.getValid();
        if (result.getValid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (allValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/allOf"), baseLocation + "/allOf", "Instance does not match every subschema", OutputErrorType.INVALID_VALUE));
      }
    }

    if (schema.containsKey("oneOf")) {
      final int errorsLength = errors.size();
      int matches = 0;
      for (int i = 0; i < schema.<JsonArray>get("oneOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("oneOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          schemaLocation + "/oneOf/" + i,
          baseLocation + "/oneOf/" + i,
          subEvaluated,
          dynamicContext
        );
        if (result.getErrors() != null) {
          errors.addAll(result.getErrors());
        }
        if (result.getValid()) {
          subEvaluateds.addAll(subEvaluated);
        }
        if (result.getValid()) {
          matches++;
        }
      }
      if (matches == 1) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/oneOf"), baseLocation + "/oneOf", "Instance does not match exactly one subschema (" + matches + " matches)", OutputErrorType.INVALID_VALUE));
      }
    }

    if ("object".equals(instanceType) || "array".equals(instanceType)) {
      evaluated.addAll(subEvaluateds);
    }

    if (schema.containsKey("if")) {
      final OutputUnit conditionResult = validate(
        instance,
        Schemas.wrap((JsonObject) schema, "if"),
        recursiveAnchor,
        instanceLocation,
        schemaLocation + "/if",
        baseLocation + "/if",
        evaluated,
        dynamicContext
      );
      if (conditionResult.getValid()) {
        if (schema.containsKey("then")) {
          final OutputUnit thenResult = validate(
            instance,
            Schemas.wrap((JsonObject) schema, "then"),
            recursiveAnchor,
            instanceLocation,
            schemaLocation + "/then",
            baseLocation + "/then",
            evaluated,
            dynamicContext
          );
          if (!thenResult.getValid()) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/if"), baseLocation + "/if", "Instance does not match \"then\" schema", thenResult.getErrorType()));
            if (thenResult.getErrors() != null) {
              errors.addAll(thenResult.getErrors());
            }
          }
        }
      } else if (schema.containsKey("else")) {
        final OutputUnit elseResult = validate(
          instance,
          Schemas.wrap((JsonObject) schema, "else"),
          recursiveAnchor,
          instanceLocation,
          schemaLocation + "/else",
          baseLocation + "/else",
          evaluated,
          dynamicContext
        );
        if (!elseResult.getValid()) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/else"), baseLocation + "/else", "Instance does not match \"else\" schema", elseResult.getErrorType()));
          if (elseResult.getErrors() != null) {
            errors.addAll(elseResult.getErrors());
          }
        }
      }
    }

    switch (instanceType) {
      case "object": {
        if (schema.containsKey("required")) {
          for (final Object key : schema.<JsonArray>get("required")) {
            if (!((JsonObject) instance).containsKey((String) key)) {
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/required"), baseLocation + "/required", "Instance does not have required property \"" + key + "\"", OutputErrorType.MISSING_VALUE));
            }
          }
        }

        final Set<String> keys = ((JsonObject) instance).fieldNames();

        if (schema.containsKey("minProperties") && keys.size() < schema.<Integer>get("minProperties")) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minProperties"), baseLocation + "/minProperties", "Instance does not have at least " + schema.get("minProperties") + " properties", OutputErrorType.MISSING_VALUE));
        }

        if (schema.containsKey("maxProperties") && keys.size() > schema.<Integer>get("maxProperties")) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maxProperties"), baseLocation + "/maxProperties", "Instance does not have at least " + schema.get("maxProperties") + " properties", OutputErrorType.INVALID_VALUE));
        }

        if (schema.containsKey("propertyNames")) {
          for (final String key : ((JsonObject) instance).fieldNames()) {
            final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
            final OutputUnit result = validate(
              key,
              Schemas.wrap((JsonObject) schema, "propertyNames"),
              recursiveAnchor,
              subInstancePointer,
              schemaLocation + "/propertyNames",
              baseLocation + "/propertyNames",
              new HashSet<>(),
              dynamicContext
            );
            if (!result.getValid()) {
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/propertyNames"), baseLocation + "/propertyNames", "Property name \"" + key + "\" does not match schema", OutputErrorType.INVALID_VALUE));
              if (result.getErrors() != null) {
                errors.addAll(result.getErrors());
              }
            }
          }
        }

        if (schema.containsKey("dependentRequired")) {
          for (final String key : schema.<JsonObject>get("dependentRequired").fieldNames()) {
            if (((JsonObject) instance).containsKey(key)) {
              final JsonArray required = schema.<JsonObject>get("dependentRequired").getJsonArray(key);
              for (final Object dependantKey : required) {
                if (!(((JsonObject) instance).containsKey((String) dependantKey))) {
                  errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/dependentRequired"), baseLocation + "/dependentRequired", "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\"", OutputErrorType.MISSING_VALUE));
                }
              }
            }
          }
        }

        if (schema.containsKey("dependentSchemas")) {
          for (final String key : schema.<JsonObject>get("dependentSchemas").fieldNames()) {
            if (((JsonObject) instance).containsKey(key)) {
              final OutputUnit result = validate(
                instance,
                Schemas.wrap(schema.get("dependentSchemas"), key),
                recursiveAnchor,
                instanceLocation,
                schemaLocation + "/dependentSchemas/" + Pointers.encode(key),
                baseLocation + "/dependentSchemas/" + Pointers.encode(key),
                evaluated,
                dynamicContext
              );
              if (!result.getValid()) {
                errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/dependentSchemas"), baseLocation + "/dependentSchemas", "Instance has \"" + key + "\" but does not match dependant schema", OutputErrorType.MISSING_VALUE));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
              }
            }
          }
        }

        if (schema.containsKey("dependencies")) {
          for (final String key : schema.<JsonObject>get("dependencies").fieldNames()) {
            if (((JsonObject) instance).containsKey(key)) {
              final Object propsOrSchema = schema.<JsonObject>get("dependencies").getValue(key);
              if (propsOrSchema instanceof JsonArray) {
                for (final Object dependantKey : ((JsonArray) propsOrSchema)) {
                  if (!((JsonObject) instance).containsKey((String) dependantKey)) {
                    errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/dependencies"), baseLocation + "/dependencies", "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\"", OutputErrorType.MISSING_VALUE));
                  }
                }
              } else {
                final OutputUnit result = validate(
                  instance,
                  Schemas.wrap(schema.get("dependencies"), key),
                  recursiveAnchor,
                  instanceLocation,
                  schemaLocation + "/dependencies/" + Pointers.encode(key),
                  baseLocation + "/dependencies/" + Pointers.encode(key),
                  new HashSet<>(),
                  dynamicContext
                );
                if (!result.getValid()) {
                  errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/dependencies"), baseLocation + "/dependencies", "Instance has \"" + key + "\" but does not match dependant schema", OutputErrorType.MISSING_VALUE));
                  if (result.getErrors() != null) {
                    errors.addAll(result.getErrors());
                  }
                }
              }
            }
          }
        }

        final Set<Object> thisEvaluated = new HashSet<>();

        boolean stop = false;

        if (schema.containsKey("properties")) {
          for (final String key : schema.<JsonObject>get("properties").fieldNames()) {
            if (!((JsonObject) instance).containsKey(key)) {
              continue;
            }
            final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
            final OutputUnit result = validate(
              ((JsonObject) instance).getValue(key),
              Schemas.wrap(schema.get("properties"), key),
              recursiveAnchor,
              subInstancePointer,
              schemaLocation + "/properties/" + Pointers.encode(key),
              baseLocation + "/properties/" + Pointers.encode(key),
              new HashSet<>(),
              dynamicContext
            );
            if (result.getValid()) {
              evaluated.add(key);
              thisEvaluated.add(key);
            } else {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(subInstancePointer, computeAbsoluteKeywordLocation(schema, schemaLocation + "/properties"), baseLocation + "/properties", "Property \"" + key + "\" does not match schema", result.getErrorType()));
              if (result.getErrors() != null) {
                errors.addAll(result.getErrors());
              }
              if (stop) {
                break;
              }
            }
          }
        }

        if (!stop && schema.containsKey("patternProperties")) {
          for (final String pattern : schema.<JsonObject>get("patternProperties").fieldNames()) {
            final Pattern regex = Pattern.compile(pattern);
            for (final String key : ((JsonObject) instance).fieldNames()) {
              if (!regex.matcher(key).find()) {
                continue;
              }
              final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
              final OutputUnit result = validate(
                ((JsonObject) instance).getValue(key),
                Schemas.wrap(schema.get("patternProperties"), pattern),
                recursiveAnchor,
                subInstancePointer,
                schemaLocation + "/patternProperties/" + Pointers.encode(pattern),
                baseLocation + "/patternProperties/" + Pointers.encode(pattern),
                new HashSet<>(),
                dynamicContext
              );
              if (result.getValid()) {
                evaluated.add(key);
                thisEvaluated.add(key);
              } else {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(subInstancePointer, computeAbsoluteKeywordLocation(schema, schemaLocation + "/patternProperties"), baseLocation + "/patternProperties", "Property \"" + key + "\" matches pattern \"" + pattern + "\" but does not match associated schema", OutputErrorType.MISSING_VALUE));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
              }
            }
          }
        }

        if (!stop && schema.containsKey("additionalProperties")) {
          for (final String key : ((JsonObject) instance).fieldNames()) {
            if (thisEvaluated.contains(key)) {
              continue;
            }
            final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
            final OutputUnit result = validate(
              ((JsonObject) instance).getValue(key),
              Schemas.wrap((JsonObject) schema, "additionalProperties"),
              recursiveAnchor,
              subInstancePointer,
              schemaLocation + "/additionalProperties",
              baseLocation + "/additionalProperties",
              new HashSet<>(),
              dynamicContext
            );
            if (result.getValid()) {
              evaluated.add(key);
            } else {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(subInstancePointer, computeAbsoluteKeywordLocation(schema, schemaLocation + "/additionalProperties"), baseLocation + "/additionalProperties", "Property \"" + key + "\" does not match additional properties schema", result.getErrorType()));
              if (result.getErrors() != null) {
                errors.addAll(result.getErrors());
              }
              if (stop) {
                break;
              }
            }
          }
        } else if (!stop && schema.containsKey("unevaluatedProperties")) {
          for (final String key : ((JsonObject) instance).fieldNames()) {
            if (!evaluated.contains(key)) {
              final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
              final OutputUnit result = validate(
                ((JsonObject) instance).getValue(key),
                Schemas.wrap((JsonObject) schema, "unevaluatedProperties"),
                recursiveAnchor,
                subInstancePointer,
                schemaLocation + "/unevaluatedProperties",
                baseLocation + "/unevaluatedProperties",
                new HashSet<>(),
                dynamicContext
              );
              if (result.getValid()) {
                evaluated.add(key);
              } else {
                errors.add(new OutputUnit(subInstancePointer, computeAbsoluteKeywordLocation(schema, schemaLocation + "/unevaluatedProperties"), baseLocation + "/unevaluatedProperties", "Property \"" + key + "\" does not match unevaluated properties schema", result.getErrorType()));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
              }
            }
          }
        }
        break;
      }
      case "array": {
        if (schema.containsKey("maxItems") && ((JsonArray) instance).size() > schema.<Integer>get("maxItems")) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maxItems"), baseLocation + "/maxItems", "Array has too many items ( + " + ((JsonArray) instance).size() + " > " + schema.get("maxItems") + ")", OutputErrorType.INVALID_VALUE));
        }

        if (schema.containsKey("minItems") && ((JsonArray) instance).size() < schema.<Integer>get("minItems")) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minItems"), baseLocation + "/minItems", "Array has too few items ( + " + ((JsonArray) instance).size() + " < " + schema.get("minItems") + ")", OutputErrorType.MISSING_VALUE));
        }

        final int length = ((JsonArray) instance).size();
        int i = 0;
        boolean stop = false;

        if (schema.containsKey("prefixItems")) {
          final int length2 = Math.min(schema.<JsonArray>get("prefixItems").size(), length);
          for (; i < length2; i++) {
            final OutputUnit result = validate(
              ((JsonArray) instance).getValue(i),
              Schemas.wrap(schema.get("prefixItems"), i),
              recursiveAnchor,
              instanceLocation + "/" + i,
              schemaLocation + "/prefixItems/" + i,
              baseLocation + "/prefixItems/" + i,
              new HashSet<>(),
              dynamicContext
            );
            evaluated.add(i);
            if (!result.getValid()) {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/prefixItems"), baseLocation + "/prefixItems", "Items did not match schema", result.getErrorType()));
              if (result.getErrors() != null) {
                errors.addAll(result.getErrors());
              }
              if (stop) {
                break;
              }
            }
          }
        }

        if (schema.containsKey("items")) {
          if (schema.get("items") instanceof JsonArray) {
            final int length2 = Math.min(schema.<JsonArray>get("items").size(), length);
            for (; i < length2; i++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(i),
                Schemas.wrap(schema.get("items"), i),
                recursiveAnchor,
                instanceLocation + "/" + i,
                schemaLocation + "/items/" + i,
                baseLocation + "/items/" + i,
                new HashSet<>(),
                dynamicContext
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/items"), baseLocation + "/items", "Items did not match schema", result.getErrorType()));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
                if (stop) {
                  break;
                }
              }
            }
          } else {
            for (; i < length; i++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(i),
                Schemas.wrap((JsonObject) schema, "items"),
                recursiveAnchor,
                instanceLocation + "/" + i,
                schemaLocation + "/items",
                baseLocation + "/items",
                new HashSet<>(),
                dynamicContext
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/items"), baseLocation + "/items", "Items did not match schema", result.getErrorType()));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
                if (stop) {
                  break;
                }
              }
            }
          }

          if (!stop && schema.containsKey("additionalItems")) {
            final String keywordLocation2 = schemaLocation + "/additionalItems";
            for (; i < length; i++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(i),
                Schemas.wrap((JsonObject) schema, "additionalItems"),
                recursiveAnchor,
                instanceLocation + "/" + i,
                keywordLocation2,
                baseLocation + "/additionalItems",
                new HashSet<>(),
                dynamicContext
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/additionalItems"), schemaLocation + "/additionalItems", "Items did not match additional items schema", result.getErrorType()));
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
              }
            }
          }
        }

        if (schema.containsKey("contains")) {
          if (length == 0 && !schema.containsKey("minContains")) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/contains"), baseLocation + "/contains", "Array is empty. It must contain at least one item matching the schema", OutputErrorType.MISSING_VALUE));
          } else if (schema.containsKey("minContains") && length < schema.<Integer>get("minContains")) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minContains"), baseLocation + "/minContains", "Array has less items (" + length + ") than minContains (" + schema.get("minContains") + ")", OutputErrorType.MISSING_VALUE));
          } else {
            final int errorsLength = errors.size();
            int contained = 0;
            for (int j = 0; j < length; j++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(j),
                Schemas.wrap((JsonObject) schema, "contains"),
                recursiveAnchor,
                instanceLocation + "/" + i,
                schemaLocation + "/contains",
                baseLocation + "/contains",
                new HashSet<>(),
                dynamicContext
              );
              if (result.getValid()) {
                evaluated.add(j);
                contained++;
              } else {
                if (result.getErrors() != null) {
                  errors.addAll(result.getErrors());
                }
              }
            }

            if (contained >= schema.<Integer>get("minContains", 0)) {
              errors = errors.subList(0, Math.min(errors.size(), errorsLength));
            }

            if (
              !schema.containsKey("minContains") &&
                !schema.containsKey("maxContains") &&
                contained == 0
            ) {
              errors.add(errorsLength, new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/contains"), baseLocation + "/contains", "Array does not contain item matching schema", OutputErrorType.INVALID_VALUE));
            } else if (schema.containsKey("minContains") && contained < schema.<Integer>get("minContains")) {
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minContains"), baseLocation + "/minContains", "Array must contain at least " + schema.get("minContains") + " items matching schema. Only " + contained + " items were found", OutputErrorType.MISSING_VALUE));
            } else if (schema.containsKey("maxContains") && contained > schema.<Integer>get("maxContains")) {
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maxContains"), baseLocation + "/maxContains", "Array may contain at most " + schema.get("minContains") + " items matching schema. " + contained + " items were found", OutputErrorType.INVALID_VALUE));
            }
          }
        }

        if (!stop && schema.containsKey("unevaluatedItems")) {
          for (; i < length; i++) {
            if (evaluated.contains(i)) {
              continue;
            }
            final OutputUnit result = validate(
              ((JsonArray) instance).getValue(i),
              Schemas.wrap((JsonObject) schema, "unevaluatedItems"),
              recursiveAnchor,
              instanceLocation + "/" + i,
              schemaLocation + "/unevaluatedItems",
              baseLocation + "/unevaluatedItems",
              new HashSet<>(),
              dynamicContext
            );
            evaluated.add(i);
            if (!result.getValid()) {
              errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/unevaluatedItems"), baseLocation + "/unevaluatedItems", "Items did not match unevaluated items schema", result.getErrorType()));
              if (result.getErrors() != null) {
                errors.addAll(result.getErrors());
              }
            }
          }
        }

        if (schema.containsKey("uniqueItems") && Utils.Objects.truthy(schema.get("uniqueItems"))) {
          outer:
          for (int j = 0; j < length; j++) {
            final Object a = ((JsonArray) instance).getValue(j);
            final boolean ao = "object".equals(JSON.typeOf(a)) && a != null;
            for (int k = 0; k < length; k++) {
              if (j == k) {
                continue;
              }
              final Object b = ((JsonArray) instance).getValue(k);
              final boolean bo = "object".equals(JSON.typeOf(b)) && b != null;
              if (Utils.Objects.equals(a, b) || (ao && bo && JSON.deepCompare(a, b))) {
                errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/uniqueItems"), baseLocation + "/uniqueItems", "Duplicate items at indexes " + j + " and " + k, OutputErrorType.INVALID_VALUE));
                break outer;
              }
            }
          }
        }
        break;
      }
      case "number":
        if (draft == Draft.DRAFT4) {
          if (
            schema.containsKey("minimum") &&
              ((schema.<Boolean>get("exclusiveMinimum", false) && Numbers.lte((Number) instance, schema.get("minimum"))) ||
                Numbers.lt((Number) instance, schema.get("minimum")))
          ) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minimum"), baseLocation + "/minimum", instance + " is less than " + (schema.<Boolean>get("exclusiveMinimum", false) ? "or equal to " : "") + schema.get("minimum"), OutputErrorType.INVALID_VALUE));
          }
          if (
            schema.containsKey("maximum") &&
              ((schema.<Boolean>get("exclusiveMaximum", false) && Numbers.gte((Number) instance, schema.get("maximum"))) ||
                Numbers.gt((Number) instance, schema.get("maximum")))
          ) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maximum"), baseLocation + "/maximum", instance + " is greater than " + (schema.<Boolean>get("exclusiveMaximum", false) ? "or equal to " : "") + schema.get("maximum"), OutputErrorType.INVALID_VALUE));
          }
        } else {
          if (schema.containsKey("minimum") && Numbers.lt((Number) instance, schema.get("minimum"))) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minimum"), baseLocation + "/minimum", instance + " is less than " + schema.get("minimum"), OutputErrorType.INVALID_VALUE));
          }
          if (schema.containsKey("maximum") && Numbers.gt((Number) instance, schema.get("maximum"))) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maximum"), baseLocation + "/maximum", instance + " is greater than " + schema.get("maximum"), OutputErrorType.INVALID_VALUE));
          }
          if (schema.containsKey("exclusiveMinimum") && Numbers.lte((Number) instance, schema.get("exclusiveMinimum"))) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/exclusiveMinimum"), baseLocation + "/exclusiveMinimum", instance + " is less than or equal to " + schema.get("exclusiveMinimum"), OutputErrorType.INVALID_VALUE));
          }
          if (schema.containsKey("exclusiveMaximum") && Numbers.gte((Number) instance, schema.get("exclusiveMaximum"))) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/exclusiveMaximum"), baseLocation + "/exclusiveMaximum", instance + " is greater than or equal to " + schema.get("exclusiveMaximum"), OutputErrorType.INVALID_VALUE));
          }
        }
        if (schema.containsKey("multipleOf")) {
          final double remainder = Numbers.remainder((Number) instance, schema.get("multipleOf"));
          if (
            Math.abs(0 - remainder) >= 1.1920929e-7 &&
              Math.abs(schema.<Number>get("multipleOf").doubleValue() - remainder) >= 1.1920929e-7
          ) {
            errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/multipleOf"), baseLocation + "/multipleOf", instance + " is not a multiple of " + schema.get("multipleOf"), OutputErrorType.INVALID_VALUE));
          }
        }
        break;
      case "string": {
        final int length =
          !schema.containsKey("minLength") && !schema.containsKey("maxLength")
            ? 0
            : Strings.ucs2length((String) instance);
        if (schema.containsKey("minLength") && Numbers.lt(length, schema.get("minLength"))) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/minLength"), baseLocation + "/minLength", "String is too short (" + length + " < " + schema.get("minLength") + ")", OutputErrorType.INVALID_VALUE));
        }
        if (schema.containsKey("maxLength") && Numbers.gt(length, schema.get("maxLength"))) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/maxLength"), baseLocation + "/maxLength", "String is too long (" + length + " > " + schema.get("maxLength") + ")", OutputErrorType.INVALID_VALUE));
        }
        if (schema.containsKey("pattern") && !Pattern.compile(schema.get("pattern")).matcher((String) instance).find()) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/pattern"), baseLocation + "/pattern", "String does not match pattern", OutputErrorType.INVALID_VALUE));
        }
        if (
          schema.containsKey("format") &&
            !Format.fastFormat(schema.get("format"), (String) instance)
        ) {
          errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/format"), baseLocation + "/format", "String does not match format \"" + schema.get("format") + "\"", OutputErrorType.INVALID_VALUE));
        }
        break;
      }
    }

    String error = formatValidator.validateFormat(instanceType, schema.get("format"), instance);
    if (error != null) {
      errors.add(new OutputUnit(instanceLocation, computeAbsoluteKeywordLocation(schema, schemaLocation + "/format"),
        baseLocation + "/format", error, OutputErrorType.INVALID_VALUE));
    }

    if (dynamicAnchor != null) {
      dynamicContext
        .get(dynamicAnchor)
        .removeLast();
    }

    return new OutputUnit(errors.isEmpty())
      .setErrors(outputFormat == OutputFormat.Flag ? null : errors.isEmpty() ? null : errors)
      .setAnnotations(outputFormat == OutputFormat.Flag ? null : annotations.isEmpty() ? null : annotations)
      .setErrorType((outputFormat == OutputFormat.Flag ? OutputErrorType.NONE :
        errors.isEmpty() ? OutputErrorType.NONE : errors.get(0).getErrorType()));
  }

  private String computeAbsoluteKeywordLocation(JsonSchema schema, String schemaKeywordLocation) {
    if (schemaKeywordLocation == null) {
      return null;
    }

    final String absoluteUri = schema.get("__absolute_uri__");

    if (absoluteUri == null) {
      return null;
    }

    return new URL(schemaKeywordLocation, absoluteUri).href();
  }
}
