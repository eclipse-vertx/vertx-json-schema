package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.*;

import java.util.*;
import java.util.regex.Pattern;

import static io.vertx.json.schema.validator.impl.AbstractSchema.wrap;
import static io.vertx.json.schema.validator.impl.Utils.*;

public class ValidatorImpl implements Validator {

  private static final List<String> IGNORE_KEYWORD = Arrays.asList(
    "id",
    "$id",
    "$ref",
    "$schema",
    "$anchor",
    "$vocabulary",
    "$comment",
    "default",
    "enum",
    "const",
    "required",
    "type",
    "maximum",
    "minimum",
    "exclusiveMaximum",
    "exclusiveMinimum",
    "multipleOf",
    "maxLength",
    "minLength",
    "pattern",
    "format",
    "maxItems",
    "minItems",
    "uniqueItems",
    "maxProperties",
    "minProperties");

  private static final List<String> SCHEMA_ARRAY_KEYWORD = Arrays.asList(
    "prefixItems",
    "items",
    "allOf",
    "anyOf",
    "oneOf");

  private static final List<String> SCHEMA_MAP_KEYWORD = Arrays.asList(
    "$defs",
    "definitions",
    "properties",
    "patternProperties",
    "dependentSchemas");

  private final Map<String, Schema<?>> lookup;

  private final Schema<?> schema;
  private final Draft draft;
  private final boolean shortCircuit;
  private final URL baseUri;

  public ValidatorImpl(Schema<?> schema, ValidatorOptions options) {
    this.schema = schema;
    this.draft = options.getDraft();
    this.shortCircuit = options.isShortCircuit();
    this.baseUri = new URL(options.getBaseUri());
    this.lookup = dereference(schema, new HashMap<>(), baseUri, "");
  }

  @Override
  public Validator addSchema(Schema<JsonObject> schema) {
    dereference(schema, lookup, baseUri, "");
    return this;
  }

  @Override
  public Validator addRemoteSchema(Schema<JsonObject> schema, String uri) {
    dereference(schema, lookup, new URL(uri), "");
    return this;
  }

  @Override
  public ValidationResult validate(Object instance) {
    return validate(
      instance,
      schema,
      draft,
      lookup,
      shortCircuit,
      null,
      "#",
      "#",
      new HashSet<>());
  }

  public static Map<String, Schema<?>> dereference(Schema<?> schema, Map<String, Schema<?>> lookup, URL baseURI, String basePointer) {
    if (schema instanceof JsonSchema) {
      final String id = schema.get("$id", schema.get("id"));
      if (Utils.Objects.truthy(id)) {
        final URL url = new URL(id, baseURI.href());
        if (url.fragment().length() > 1) {
          lookup.put(url.href(), schema);
        } else {
          url.anchor(""); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
          if ("".equals(basePointer)) {
            baseURI = url;
          } else {
            dereference(schema, lookup, baseURI, "");
          }
        }
      }
    } else if (!(schema instanceof BooleanSchema)) {
      return lookup;
    }

    // compute the schema's URI and add it to the mapping.
    final String schemaURI = baseURI.href() + (Utils.Objects.truthy(basePointer) ? '#' + basePointer : "");
    if (lookup.containsKey(schemaURI)) {
      throw new IllegalStateException("Duplicate schema URI \"" + schemaURI + "\".");
    }
    lookup.put(schemaURI, schema);

    // exit early if this is a boolean schema.
    if (schema instanceof BooleanSchema) {
      return lookup;
    }

    // set the schema's absolute URI.
    if (!schema.contains("__absolute_uri__")) {
      schema.annotate("__absolute_uri__", schemaURI);
    }

    // if a $ref is found, resolve it's absolute URI.
    if (schema.contains("$ref") && !schema.contains("__absolute_ref__")) {
      final URL url = new URL(schema.get("$ref"), baseURI.href());
      url.anchor(url.fragment()); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
      schema.annotate("__absolute_ref__", url.href());
    }

    // if a $recursiveRef is found, resolve it's absolute URI.
    if (schema.contains("$recursiveRef") && !schema.contains("__absolute_recursive_ref__")) {
      final URL url = new URL(schema.get("$recursiveRef"), baseURI.href());
      url.anchor(url.fragment()); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
      schema.annotate("__absolute_recursive_ref__", url.href());
    }

    // if an $anchor is found, compute it's URI and add it to the mapping.
    if (schema.contains("$anchor")) {
      final URL url = new URL("#" + schema.<String>get("$anchor"), baseURI);
      lookup.put(url.href(), schema);
    }

    // process subschemas.
    for (String key : schema.keys()) {
      if (IGNORE_KEYWORD.contains(key)) {
        continue;
      }

      final String keyBase = basePointer + "/" + Pointers.encode(key);
      final Object subSchema = schema.get(key);

      if (subSchema instanceof JsonArray) {
        if (SCHEMA_ARRAY_KEYWORD.contains(key)) {
          for (int i = 0; i < ((JsonArray) subSchema).size(); i++) {
            dereference(
              wrap((JsonArray) subSchema, i),
              lookup,
              baseURI,
              keyBase + "/" + i);
          }
        }
      } else if (SCHEMA_MAP_KEYWORD.contains(key)) {
        for (String subKey : ((JsonObject) subSchema).fieldNames()) {
          dereference(
            wrap((JsonObject) subSchema, subKey),
            lookup,
            baseURI,
            keyBase + "/" + Pointers.encode(subKey));
        }
      } else if (subSchema instanceof Boolean) {
        dereference(Schema.fromBoolean((Boolean) subSchema), lookup, baseURI, keyBase);
      } else if (subSchema instanceof JsonObject) {
        dereference(Schema.fromJson((JsonObject) subSchema), lookup, baseURI, keyBase);
      }
    }

    return lookup;
  }

  public static ValidationResult validate(Object _instance, Schema<?> schema, Draft draft, Map<String, Schema<?>> lookup, boolean shortCircuit, Schema<?> _recursiveAnchor, String instanceLocation, String schemaLocation, Set<Object> evaluated) {
    if (schema.unwrap() instanceof Boolean) {
      if ((Boolean) schema.unwrap()) {
        return new ValidationResultImpl(true, Collections.emptyList());
      } else {
        return new ValidationResultImpl(false, Collections.singletonList(new ErrorUnit(instanceLocation, "false", instanceLocation, "False boolean schema")));
      }
    }

    // adapt JSON types
    final Object instance = JSON.jsonify(_instance);

    // start validating
    String instanceType = JSON.typeOf(instance);
    List<ValidationError> errors = new ArrayList<>();

    if (schema.<Boolean>get("$recursiveAnchor", false) && _recursiveAnchor == null) {
      _recursiveAnchor = schema;
    }

    // Lock
    final Schema<?> recursiveAnchor = _recursiveAnchor;

    if ("#".equals(schema.get("$recursiveRef"))) {
      assert schema.contains("__absolute_recursive_ref__");
      final Schema<?> refSchema =
        recursiveAnchor == null
          ? lookup.get(schema.<String>get("__absolute_recursive_ref__"))
          : recursiveAnchor;
      final String keywordLocation = schemaLocation + "/$recursiveRef";
      final ValidationResult result = validate(
        instance,
        recursiveAnchor == null ? schema : recursiveAnchor,
        draft,
        lookup,
        shortCircuit,
        refSchema,
        instanceLocation,
        keywordLocation,
        evaluated
      );
      if (!result.valid()) {
        errors.add(new ErrorUnit(instanceLocation, "$recursiveRef", keywordLocation, "A sub-schema had errors"));
        errors.addAll(result.errors());
      }
    }

    if (schema.contains("$ref")) {
      final String uri = schema.get("__absolute_ref__", schema.get("$ref"));
      if (!lookup.containsKey(uri)) {
        String message = "Unresolved $ref " + schema.<String>get("$ref");
        if (schema.contains("__absolute_ref__") && !schema.get("__absolute_ref__").equals(schema.<String>get("$ref"))) {
          message += ": Absolute URI " + schema.get("__absolute_ref__");
        }
        message += "\nKnown schemas:\n- " + String.join("\n- ", lookup.keySet());
        throw new IllegalStateException(message);
      }

      final Schema<?> refSchema = lookup.get(uri);
      final String keywordLocation = schemaLocation + "/" + schema.<String>get("$ref");
      final ValidationResult result = validate(
        instance,
        refSchema,
        draft,
        lookup,
        shortCircuit,
        recursiveAnchor,
        instanceLocation,
        keywordLocation,
        evaluated
      );
      if (!result.valid()) {
        errors.add(new ErrorUnit(instanceLocation, "$ref", keywordLocation, "A subschema had errors"));
        errors.addAll(result.errors());
      }
      if (draft == Draft.DRAFT4 || draft == Draft.DRAFT7) {
        return new ValidationResultImpl(errors);
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
        errors.add(new ErrorUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + String.join(", ", type.getList())));
      }
    } else if ("integer".equals(schema.get("type"))) {
      if (!"number".equals(instanceType) || !Numbers.isInteger(instance)) {
        errors.add(new ErrorUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type")));
      }
    } else if (schema.contains("type") && !instanceType.equals(schema.get("type"))) {
      errors.add(new ErrorUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type")));
    }

    if (schema.contains("const")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (!JSON.deepCompare(instance, schema.get("const"))) {
          errors.add(new ErrorUnit(instanceLocation, "const", schemaLocation + "/const", "Instance does not match " + Json.encode(schema.get("const"))));
        }
      } else if (!Utils.Objects.equals(schema.get("const"), instance)) {
        errors.add(new ErrorUnit(instanceLocation, "const", schemaLocation + "/const", "Instance does not match " + Json.encode(schema.get("const"))));
      }
    }

    if (schema.contains("enum")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (!schema.<JsonArray>get("enum").stream().anyMatch(value -> JSON.deepCompare(instance, value))) {
          errors.add(new ErrorUnit(instanceLocation, "enum", schemaLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum"))));
        }
      } else if (!schema.<JsonArray>get("enum").stream().anyMatch(value -> Utils.Objects.equals(instance, value))) {
        errors.add(new ErrorUnit(instanceLocation, "enum", schemaLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum"))));
      }
    }

    if (schema.contains("not")) {
      final String keywordLocation = schemaLocation + "/not";
      final ValidationResult result = validate(
        instance,
        wrap(schema, "not"),
        draft,
        lookup,
        shortCircuit,
        recursiveAnchor,
        instanceLocation,
        keywordLocation,
        new HashSet<>()
      );
      if (result.valid()) {
        errors.add(new ErrorUnit(instanceLocation, "not", keywordLocation, "Instance matched \"not\" schema"));
      }
    }

    Set<Object> subEvaluateds = new HashSet<>();

    if (schema.contains("anyOf")) {
      final String keywordLocation = schemaLocation + "/anyOf";
      final int errorsLength = errors.size();
      boolean anyValid = false;
      for (int i = 0; i < schema.<JsonArray>get("anyOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final ValidationResult result = validate(
          instance,
          wrap(schema.<JsonArray>get("anyOf"), i),
          draft,
          lookup,
          shortCircuit,
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.errors());
        anyValid = anyValid || result.valid();
        if (result.valid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (anyValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new ErrorUnit(instanceLocation, "anyOf", keywordLocation, "Instance does not match any subschemas"));
      }
    }

    if (schema.contains("allOf")) {
      final String keywordLocation = schemaLocation + "/allOf";
      final int errorsLength = errors.size();
      boolean allValid = true;
      for (int i = 0; i < schema.<JsonArray>get("allOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final ValidationResult result = validate(
          instance,
          wrap(schema.<JsonArray>get("allOf"), i),
          draft,
          lookup,
          shortCircuit,
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.errors());
        allValid = allValid && result.valid();
        if (result.valid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (allValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new ErrorUnit(instanceLocation, "allOf", keywordLocation, "Instance does not match every subschema"));
      }
    }

    if (schema.contains("oneOf")) {
      final String keywordLocation = schemaLocation + "/oneOf";
      final int errorsLength = errors.size();
      int matches = 0;
      for (int i = 0; i < schema.<JsonArray>get("oneOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final ValidationResult result = validate(
          instance,
          wrap(schema.<JsonArray>get("oneOf"), i),
          draft,
          lookup,
          shortCircuit,
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.errors());
        if (result.valid()) {
          subEvaluateds.addAll(subEvaluated);
        }
        if (result.valid()) {
          matches++;
        }
      }
      if (matches == 1) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new ErrorUnit(instanceLocation, "oneOf", keywordLocation, "Instance does not match exactly one subschema (" + matches + " matches)"));
      }
    }

    if ("object".equals(instanceType) || "array".equals(instanceType)) {
      evaluated.addAll(subEvaluateds);
    }

  if (schema.contains("if")) {
    final String keywordLocation = schemaLocation + "/if";
    final ValidationResult conditionResult = validate(
      instance,
      wrap(schema, "if"),
      draft,
      lookup,
      shortCircuit,
      recursiveAnchor,
      instanceLocation,
      keywordLocation,
      evaluated
    );
    if (conditionResult.valid()) {
      if (schema.contains("then")) {
        final ValidationResult thenResult = validate(
          instance,
          wrap(schema, "then"),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          instanceLocation,
          schemaLocation + "/then",
          evaluated
        );
        if (!thenResult.valid()) {
          errors.add(new ErrorUnit(instanceLocation, "if", keywordLocation, "Instance does not match \"then\" schema"));
          errors.addAll(thenResult.errors());
        }
      }
    } else if (schema.contains("else")) {
      final ValidationResult elseResult = validate(
        instance,
        wrap(schema, "else"),
        draft,
        lookup,
        shortCircuit,
        recursiveAnchor,
        instanceLocation,
        schemaLocation + "/else",
        evaluated
      );
      if (!elseResult.valid()) {
        errors.add(new ErrorUnit(instanceLocation, "if", keywordLocation, "Instance does not match \"else\" schema"));
        errors.addAll(elseResult.errors());
      }
    }
  }

  if ("object".equals(instanceType)) {
    if (schema.contains("required")) {
      for (final Object key : schema.<JsonArray>get("required")) {
        if (!((JsonObject) instance).containsKey((String) key)) {
          errors.add(new ErrorUnit(instanceLocation, "required", schemaLocation + "/required", "Instance does not have required property \"" + key + "\""));
        }
      }
    }

    final Set<String> keys = ((JsonObject) instance).fieldNames();

    if (schema.contains("minProperties") && keys.size() < schema.<Integer>get("minProperties")) {
      errors.add(new ErrorUnit(instanceLocation, "minProperties", schemaLocation + "/minProperties", "Instance does not have at least " + schema.get("minProperties") + " properties"));
    }

    if (schema.contains("maxProperties") && keys.size() > schema.<Integer>get("maxProperties")) {
      errors.add(new ErrorUnit(instanceLocation, "maxProperties", schemaLocation + "/maxProperties", "Instance does not have at least " + schema.get("maxProperties") + " properties"));
    }

    if (schema.contains("propertyNames")) {
      final String keywordLocation = schemaLocation + "/propertyNames";
      for (final String key : ((JsonObject) instance).fieldNames()) {
        final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
        final ValidationResult result = validate(
          key,
          wrap(schema, "propertyNames"),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          subInstancePointer,
          keywordLocation,
          new HashSet<>()
        );
        if (!result.valid()) {
          errors.add(new ErrorUnit(instanceLocation, "propertyNames", keywordLocation, "Property name \"" + key + "\" does not match schema"));
          errors.addAll(result.errors());
        }
      }
    }

    if (schema.contains("dependentRequired")) {
      final String keywordLocation = schemaLocation + "/dependantRequired";
      for (final String key : schema.<JsonObject>get("dependentRequired").fieldNames()) {
        if (((JsonObject) instance).containsKey(key)) {
          final JsonArray required = schema.<JsonObject>get("dependentRequired").getJsonArray(key);
          for (final Object dependantKey : required) {
            if (!(((JsonObject) instance).containsKey((String) dependantKey))) {
              errors.add(new ErrorUnit(instanceLocation, "dependentRequired", keywordLocation, "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\""));
            }
          }
        }
      }
    }

    if (schema.contains("dependentSchemas")) {
      for (final String key : schema.<JsonObject>get("dependentSchemas").fieldNames()) {
        final String keywordLocation = schemaLocation + "/dependentSchemas";
        if (((JsonObject) instance).containsKey(key)) {
          final ValidationResult result = validate(
            instance,
            wrap(schema.<JsonObject>get("dependentSchemas"), key),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            instanceLocation,
            keywordLocation + "/" + Pointers.encode(key),
            evaluated
          );
          if (!result.valid()) {
            errors.add(new ErrorUnit(instanceLocation, "dependentSchemas", keywordLocation, "Instance has \"" + key + "\" but does not match dependant schema"));
            errors.addAll(result.errors());
          }
        }
      }
    }

    if (schema.contains("dependencies")) {
      final String keywordLocation = schemaLocation + "/dependencies";
      for (final String key : schema.<JsonObject>get("dependencies").fieldNames()) {
        if (((JsonObject) instance).containsKey(key)) {
          final Object propsOrSchema = schema.<JsonObject>get("dependencies").getValue(key);
          if (propsOrSchema instanceof JsonArray) {
            for (final Object dependantKey : ((JsonArray) propsOrSchema)) {
              if (!((JsonObject) instance).containsKey((String) dependantKey)) {
                errors.add(new ErrorUnit(instanceLocation, "dependencies", keywordLocation, "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\""));
              }
            }
          } else {
            final ValidationResult result = validate(
              instance,
              wrap(schema.<JsonObject>get("dependencies"), key),
              draft,
              lookup,
              shortCircuit,
              recursiveAnchor,
              instanceLocation,
              keywordLocation + "/" + Pointers.encode(key),
              new HashSet<>()
            );
            if (!result.valid()) {
              errors.add(new ErrorUnit(instanceLocation, "dependencies", keywordLocation, "Instance has \"" + key + "\" but does not match dependant schema"));
              errors.addAll(result.errors());
            }
          }
        }
      }
    }

    final Set<Object> thisEvaluated = new HashSet<>();

    boolean stop = false;

    if (schema.contains("properties")) {
      final String keywordLocation = schemaLocation + "/properties";
      for (final String key : schema.<JsonObject>get("properties").fieldNames()) {
        if (!((JsonObject) instance).containsKey(key)) {
          continue;
        }
        final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
        final ValidationResult result = validate(
          ((JsonObject) instance).getValue(key),
          wrap(schema.<JsonObject>get("properties"), key),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          subInstancePointer,
          keywordLocation + "/" + Pointers.encode(key),
          new HashSet<>()
        );
        if (result.valid()) {
          evaluated.add(key);
          thisEvaluated.add(key);
        } else {
          stop = shortCircuit;
          errors.add(new ErrorUnit(instanceLocation, "properties", keywordLocation, "Property \"" + key + "\" does not match schema"));
          errors.addAll(result.errors());
          if (stop) {
            break;
          }
        }
      }
    }

    if (!stop && schema.contains("patternProperties")) {
      final String keywordLocation = schemaLocation + "/patternProperties";
      for (final String pattern : schema.<JsonObject>get("patternProperties").fieldNames()) {
        final Pattern regex = Pattern.compile(pattern);
        for (final String key : ((JsonObject) instance).fieldNames()) {
          if (!regex.matcher(key).find()) {
            continue;
          }
          final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
          final ValidationResult result = validate(
            ((JsonObject) instance).getValue(key),
            wrap(schema.<JsonObject>get("patternProperties"), pattern),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            subInstancePointer,
            keywordLocation + "/" + Pointers.encode(pattern),
            new HashSet<>()
          );
          if (result.valid()) {
            evaluated.add(key);
            thisEvaluated.add(key);
          } else {
            stop = shortCircuit;
            errors.add(new ErrorUnit(instanceLocation, "patternProperties", keywordLocation, "Property \"" + key + "\" matches pattern \"" + pattern + "\" but does not match associated schema"));
            errors.addAll(result.errors());
          }
        }
      }
    }

    if (!stop && schema.contains("additionalProperties")) {
      final String keywordLocation = schemaLocation + "/additionalProperties";
      for (final String key : ((JsonObject) instance).fieldNames()) {
        if (thisEvaluated.contains(key)) {
          continue;
        }
        final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
        final ValidationResult result = validate(
          ((JsonObject) instance).getValue(key),
          wrap(schema, "additionalProperties"),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          subInstancePointer,
          keywordLocation,
          new HashSet<>()
        );
        if (result.valid()) {
          evaluated.add(key);
        } else {
          stop = shortCircuit;
          errors.add(new ErrorUnit(instanceLocation, "additionalProperties", keywordLocation, "Property \"" + key + "\" does not match additional properties schema"));
          errors.addAll(result.errors());
        }
      }
    } else if (!stop && schema.contains("unevaluatedProperties")) {
      final String keywordLocation = schemaLocation + "/unevaluatedProperties";
      for (final String key : ((JsonObject) instance).fieldNames()) {
        if (!evaluated.contains(key)) {
          final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
          final ValidationResult result = validate(
            ((JsonObject) instance).getValue(key),
            wrap(schema, "unevaluatedProperties"),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            subInstancePointer,
            keywordLocation,
            new HashSet<>()
          );
          if (result.valid()) {
            evaluated.add(key);
          } else {
            errors.add(new ErrorUnit(instanceLocation, "unevaluatedProperties", keywordLocation, "Property \"" + key + "\" does not match unevaluated properties schema"));
            errors.addAll(result.errors());
          }
        }
      }
    }
  }
  else if ("array".equals(instanceType)) {
    if (schema.contains("maxItems") && ((JsonArray) instance).size() > schema.<Integer>get("maxItems")) {
      errors.add(new ErrorUnit(instanceLocation, "maxItems", schemaLocation + "/maxItems", "Array has too many items ( + " + ((JsonArray) instance).size() + " > " + schema.get("maxItems") + ")"));
    }

    if (schema.contains("minItems") && ((JsonArray) instance).size() < schema.<Integer>get("minItems")) {
      errors.add(new ErrorUnit(instanceLocation, "minItems", schemaLocation + "/minItems", "Array has too few items ( + " + ((JsonArray) instance).size() + " < " + schema.get("minItems") + ")"));
    }

    final int length = ((JsonArray) instance).size();
    int i = 0;
    boolean stop = false;

    if (schema.contains("prefixItems")) {
      final String keywordLocation = schemaLocation + "/prefixItems";
      final int length2 = Math.min(schema.<JsonArray>get("prefixItems").size(), length);
      for (; i < length2; i++) {
        final ValidationResult result = validate(
          ((JsonArray) instance).getValue(i),
          wrap(schema.<JsonArray>get("prefixItems"), i),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          instanceLocation + "/" + i,
          keywordLocation + "/" + i,
          new HashSet<>()
        );
        evaluated.add(i);
        if (!result.valid()) {
          stop = shortCircuit;
          errors.add(new ErrorUnit(instanceLocation, "prefixItems", keywordLocation, "Items did not match schema"));
          errors.addAll(result.errors());
          if (stop) {
            break;
          }
        }
      }
    }

    if (schema.contains("items")) {
      final String keywordLocation = schemaLocation + "/items";
      if (schema.get("items") instanceof JsonArray) {
        final int length2 = Math.min(schema.<JsonArray>get("items").size(), length);
        for (; i < length2; i++) {
          final ValidationResult result = validate(
            ((JsonArray) instance).getValue(i),
            wrap(schema.<JsonArray>get("items"), i),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            instanceLocation + "/" + i,
            keywordLocation + "/" + i,
            new HashSet<>()
          );
          evaluated.add(i);
          if (!result.valid()) {
            stop = shortCircuit;
            errors.add(new ErrorUnit(instanceLocation, "items", keywordLocation, "Items did not match schema"));
            errors.addAll(result.errors());
            if (stop) {
              break;
            }
          }
        }
      } else {
        for (; i < length; i++) {
          final ValidationResult result = validate(
            ((JsonArray) instance).getValue(i),
            wrap(schema, "items"),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            instanceLocation + "/" + i,
            keywordLocation,
            new HashSet<>()
          );
          evaluated.add(i);
          if (!result.valid()) {
            stop = shortCircuit;
            errors.add(new ErrorUnit(instanceLocation, "items", keywordLocation, "Items did not match schema"));
            errors.addAll(result.errors());
            if (stop) {
              break;
            }
          }
        }
      }

      if (!stop && schema.contains("additionalItems")) {
        final String keywordLocation2 = schemaLocation + "/additionalItems";
        for (; i < length; i++) {
          final ValidationResult result = validate(
            ((JsonArray) instance).getValue(i),
            wrap(schema, "additionalItems"),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            instanceLocation + "/" + i,
            keywordLocation2,
            new HashSet<>()
          );
          evaluated.add(i);
          if (!result.valid()) {
            stop = shortCircuit;
            errors.add(new ErrorUnit(instanceLocation, "additionalItems", keywordLocation2, "Items did not match additional items schema"));
            errors.addAll(result.errors());
          }
        }
      }
    }

    if (schema.contains("contains")) {
      if (length == 0 && !schema.contains("minContains")) {
        errors.add(new ErrorUnit(instanceLocation, "contains", schemaLocation + "/contains", "Array is empty. It must contain at least one item matching the schema"));
      } else if (schema.contains("minContains") && length < schema.<Integer>get("minContains")) {
        errors.add(new ErrorUnit(instanceLocation, "minContains", schemaLocation + "/minContains", "Array has less items (" + length + ") than minContains (" + schema.get("minContains") + ")"));
      } else {
        final String keywordLocation = schemaLocation + "/contains";
        final int errorsLength = errors.size();
        int contained = 0;
        for (int j = 0; j < length; j++) {
          final ValidationResult result = validate(
            ((JsonArray) instance).getValue(j),
            wrap(schema, "contains"),
            draft,
            lookup,
            shortCircuit,
            recursiveAnchor,
            instanceLocation + "/" + i,
            keywordLocation,
            new HashSet<>()
          );
          if (result.valid()) {
            evaluated.add(j);
            contained++;
          } else {
            errors.addAll(result.errors());
          }
        }

        if (contained >= schema.<Integer>get("minContains", 0)) {
          errors = errors.subList(0, Math.min(errors.size(), errorsLength));
        }

        if (
          !schema.contains("minContains") &&
          !schema.contains("maxContains") &&
          contained == 0
        ) {
          errors.add(errorsLength, new ErrorUnit(instanceLocation, "contains", keywordLocation, "Array does not contain item matching schema"));
        } else if (schema.contains("minContains") && contained < schema.<Integer>get("minContains")) {
          errors.add(new ErrorUnit(instanceLocation, "minContains", keywordLocation + "/minContains", "Array must contain at least " + schema.get("minContains") + " items matching schema. Only " + contained + " items were found"));
        } else if (schema.contains("maxContains") && contained > schema.<Integer>get("maxContains")) {
          errors.add(new ErrorUnit(instanceLocation, "maxContains", keywordLocation + "/maxContains", "Array may contain at most " + schema.get("minContains") + " items matching schema. " + contained + " items were found"));
        }
      }
    }

    if (!stop && schema.contains("unevaluatedItems")) {
      final String keywordLocation = schemaLocation + "/unevaluatedItems";
      for (; i < length; i++) {
        if (evaluated.contains(i)) {
          continue;
        }
        final ValidationResult result = validate(
          ((JsonArray) instance).getValue(i),
          wrap(schema, "unevaluatedItems"),
          draft,
          lookup,
          shortCircuit,
          recursiveAnchor,
          instanceLocation + "/" + i,
          keywordLocation,
          new HashSet<>()
        );
        evaluated.add(i);
        if (!result.valid()) {
          errors.add(new ErrorUnit(instanceLocation, "unevaluatedItems", keywordLocation, "Items did not match unevaluated items schema"));
          errors.addAll(result.errors());
        }
      }
    }

    if (schema.contains("uniqueItems") && Utils.Objects.truthy(schema.get("uniqueItems"))) {
      outer: for (int j = 0; j < length; j++) {
        final Object a = ((JsonArray) instance).getValue(j);
        final boolean ao = "object".equals(JSON.typeOf(a)) && a != null;
        for (int k = 0; k < length; k++) {
          if (j == k) {
            continue;
          }
          final Object b = ((JsonArray) instance).getValue(k);
          final boolean bo = "object".equals(JSON.typeOf(b)) && b != null;
          if (Utils.Objects.equals(a, b) || (ao && bo && JSON.deepCompare(a, b))) {
            errors.add(new ErrorUnit(instanceLocation, "uniqueItems", schemaLocation + "/uniqueItems", "Duplicate items at indexes " + j + " and " + k));
            break outer;
          }
        }
      }
    }
  }
  else if ("number".equals(instanceType)) {
    if (draft == Draft.DRAFT4) {
      if (
        schema.contains("minimum") &&
        ((schema.<Boolean>get("exclusiveMinimum", false) && Numbers.lte((Number) instance, schema.get("minimum"))) ||
          Numbers.lt((Number) instance, schema.get("minimum")))
      ) {
        errors.add(new ErrorUnit(instanceLocation, "minimum", schemaLocation + "/minimum", instance + " is less than " + (schema.<Boolean>get("exclusiveMinimum", false) ? "or equal to " : "") + schema.get("minimum")));
      }
      if (
        schema.contains("maximum") &&
        ((schema.<Boolean>get("exclusiveMaximum", false) && Numbers.gte((Number) instance, schema.get("maximum"))) ||
          Numbers.gt((Number) instance, schema.get("maximum")))
      ) {
        errors.add(new ErrorUnit(instanceLocation, "maximum", schemaLocation + "/maximum", instance + " is greater than " + (schema.<Boolean>get("exclusiveMaximum", false) ? "or equal to " : "") + schema.get("maximum")));
      }
    } else {
      if (schema.contains("minimum") && Numbers.lt((Number) instance, schema.get("minimum"))) {
        errors.add(new ErrorUnit(instanceLocation, "minimum", schemaLocation + "/minimum", instance + " is less than " + schema.get("minimum")));
      }
      if (schema.contains("maximum") && Numbers.gt((Number) instance, schema.get("maximum"))) {
        errors.add(new ErrorUnit(instanceLocation, "maximum", schemaLocation + "/maximum", instance + " is greater than " + schema.get("maximum")));
      }
      if (schema.contains("exclusiveMinimum") && Numbers.lte((Number) instance, schema.get("exclusiveMinimum"))) {
        errors.add(new ErrorUnit(instanceLocation, "exclusiveMinimum", schemaLocation + "/exclusiveMinimum", instance + " is less than or equal to " + schema.get("exclusiveMinimum")));
      }
      if (schema.contains("exclusiveMaximum") && Numbers.gte((Number) instance, schema.get("exclusiveMaximum"))) {
        errors.add(new ErrorUnit(instanceLocation, "exclusiveMaximum", schemaLocation + "/exclusiveMaximum", instance + " is greater than or equal to " + schema.get("exclusiveMaximum")));
      }
    }
    if (schema.contains("multipleOf")) {
      final double remainder = Numbers.remainder((Number) instance, schema.get("multipleOf"));
      if (
        Math.abs(0 - remainder) >= 1.1920929e-7 &&
        Math.abs(schema.<Number>get("multipleOf").doubleValue() - remainder) >= 1.1920929e-7
      ) {
        errors.add(new ErrorUnit(instanceLocation, "multipleOf", schemaLocation + "/multipleOf", instance + " is not a multiple of " + schema.get("multipleOf")));
      }
    }
  }
  else if ("string".equals(instanceType)) {
    final int length =
      !schema.contains("minLength") && !schema.contains("maxLength")
        ? 0
        : Strings.ucs2length((String) instance);
    if (schema.contains("minLength") && Numbers.lt(length, schema.get("minLength"))) {
      errors.add(new ErrorUnit(instanceLocation, "minLength", schemaLocation + "/minLength", "String is too short (" + length + " < " + schema.get("minLength") + ")"));
    }
    if (schema.contains("maxLength") && Numbers.gt(length, schema.get("maxLength"))) {
      errors.add(new ErrorUnit(instanceLocation, "maxLength", schemaLocation + "/maxLength", "String is too long (" + length + " > " + schema.get("maxLength") + ")"));
    }
    if (schema.contains("pattern") && !Pattern.compile(schema.get("pattern")).matcher((String) instance).find()) {
      errors.add(new ErrorUnit(instanceLocation, "pattern", schemaLocation + "/pattern", "String does not match pattern"));
    }
    if (
      schema.contains("format") &&
      !Format.fastFormat(schema.get("format"), (String) instance)
    ) {
      errors.add(new ErrorUnit(instanceLocation, "format", schemaLocation + "/format", "String does not match format \"" + schema.get("format") + "\""));
    }
  }

    return new ValidationResultImpl(errors);
  }
}
