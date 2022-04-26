package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.*;

import java.util.*;
import java.util.Objects;
import java.util.regex.Pattern;

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

  private static final List<String> SCHEMA_KEYWORD = Arrays.asList(
    "additionalItems",
    "unevaluatedItems",
    "items",
    "contains",
    "additionalProperties",
    "unevaluatedProperties",
    "propertyNames",
    "not",
    "if",
    "then",
    "else"
  );

  private final Map<String, Schema> lookup = new HashMap<>();

  private final Schema schema;
  private final Draft draft;
  private final OutputFormat outputFormat;
  private final URL baseUri;

  public ValidatorImpl(Schema schema, ValidatorOptions options) {
    Objects.requireNonNull(schema, "'schema' cannot be null");
    Objects.requireNonNull(options, "'options' cannot be null");
    Objects.requireNonNull(options.getOutputFormat(), "'options.outputFormat' cannot be null");
    Objects.requireNonNull(options.getBaseUri(), "'options.baseUri' cannot be null");
    this.schema = schema;
    // extract the draft from schema when no specific draft is configured in the options
    this.draft = options.getDraft() == null ?
      Draft.fromIdentifier(schema.get("$schema")) :
      options.getDraft();
    this.outputFormat = options.getOutputFormat();
    this.baseUri = new URL(options.getBaseUri());
    // add the root schema
    addSchema(schema);
  }

  @Override
  public Validator addSchema(Schema schema) {
    dereference(schema, baseUri, "", true);
    return this;
  }

  @Override
  public Validator addSchema(String uri, Schema schema) {
    dereference(schema, new URL(uri), "", true);
    return this;
  }

  @Override
  public OutputUnit validate(Object instance) {
    return validate(
      instance,
      schema,
      null,
      "#",
      "#",
      new HashSet<>());
  }

  private void dereference(Schema schema, URL baseURI, String basePointer, boolean schemaRoot) {
    if (schema instanceof JsonSchema) {
      // This addresses the Unknown Keyword requirements, non sub-schema's with $id are to ignore the
      // given $id as it could collide with existing resolved schemas
      final String id = schemaRoot ? schema.get("$id", schema.get("id")) : null;
      if (Utils.Objects.truthy(id)) {
        final URL url = new URL(id, baseURI.href());
        if (url.fragment().length() > 1) {
          assert !lookup.containsKey(url.href());
          lookup.put(url.href(), schema);
        } else {
          url.anchor(""); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
          if ("".equals(basePointer)) {
            baseURI = url;
          } else {
            dereference(schema, baseURI, "", schemaRoot);
          }
        }
      }
    } else if (!(schema instanceof BooleanSchema)) {
      return;
    }

    // compute the schema's URI and add it to the mapping.
    final String schemaURI = baseURI.href() + (Utils.Objects.truthy(basePointer) ? '#' + basePointer : "");
    if (lookup.containsKey(schemaURI)) {
      Schema existing = lookup.get(schemaURI);
      // this schema has been processed already, skip, this is the same behavior of ajv the most complete
      // validator to my knowledge. This addresses the case where extra $id's are added and would be double
      // referenced, yet, it would be ok as they are the same sub schema
      if (existing.equals(schema)) {
        return;
      }
      throw new IllegalStateException("Duplicate schema URI \"" + schemaURI + "\".");
    }
    lookup.put(schemaURI, schema);

    // exit early if this is a boolean schema.
    if (schema instanceof BooleanSchema) {
      return;
    }

    // set the schema's absolute URI.
    if (!schema.containsKey("__absolute_uri__")) {
      schema.annotate("__absolute_uri__", schemaURI);
    }

    // if a $ref is found, resolve it's absolute URI.
    if (schema.containsKey("$ref") && !schema.containsKey("__absolute_ref__")) {
      final URL url = new URL(schema.get("$ref"), baseURI.href());
      url.anchor(url.fragment()); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
      schema.annotate("__absolute_ref__", url.href());
    }

    // if a $recursiveRef is found, resolve it's absolute URI.
    if (schema.containsKey("$recursiveRef") && !schema.containsKey("__absolute_recursive_ref__")) {
      final URL url = new URL(schema.get("$recursiveRef"), baseURI.href());
      url.anchor(url.fragment()); // normalize hash https://url.spec.whatwg.org/#dom-url-hash
      schema.annotate("__absolute_recursive_ref__", url.href());
    }

    // if an $anchor is found, compute it's URI and add it to the mapping.
    if (schema.containsKey("$anchor")) {
      final URL url = new URL("#" + schema.<String>get("$anchor"), baseURI);
      assert !lookup.containsKey(url.href());
      lookup.put(url.href(), schema);
    }

    // process subschemas.
    for (String key : schema.fieldNames()) {
      if (IGNORE_KEYWORD.contains(key)) {
        continue;
      }

      final String keyBase = basePointer + "/" + Pointers.encode(key);
      final Object subSchema = schema.get(key);

      if (subSchema instanceof JsonArray) {
        if (SCHEMA_ARRAY_KEYWORD.contains(key)) {
          for (int i = 0; i < ((JsonArray) subSchema).size(); i++) {
            dereference(
              Schemas.wrap((JsonArray) subSchema, i),
              baseURI,
              keyBase + "/" + i,
              false);
          }
        }
      } else if (SCHEMA_MAP_KEYWORD.contains(key)) {
        for (String subKey : ((JsonObject) subSchema).fieldNames()) {
          dereference(
            Schemas.wrap((JsonObject) subSchema, subKey),
            baseURI,
            keyBase + "/" + Pointers.encode(subKey),
            true);
        }
      } else if (subSchema instanceof Boolean) {
        dereference(Schema.of((Boolean) subSchema), baseURI, keyBase, SCHEMA_KEYWORD.contains(key));
      } else if (subSchema instanceof JsonObject) {
        dereference(Schema.of((JsonObject) subSchema), baseURI, keyBase, SCHEMA_KEYWORD.contains(key));
      }
    }
  }

  private OutputUnit validate(Object _instance, Schema schema, Schema _recursiveAnchor, String instanceLocation, String schemaLocation, Set<Object> evaluated) {
    if (schema instanceof BooleanSchema) {
      if (schema == BooleanSchema.TRUE) {
        return new OutputUnit(true).setErrors(Collections.emptyList());
      } else {
        return new OutputUnit(false).setErrors(Collections.singletonList(new OutputUnit(instanceLocation, "false", instanceLocation, "False boolean schema")));
      }
    }

    // adapt JSON types
    final Object instance = JSON.jsonify(_instance);

    // start validating
    String instanceType = JSON.typeOf(instance);
    List<OutputUnit> errors = new ArrayList<>();

    if (schema.<Boolean>get("$recursiveAnchor", false) && _recursiveAnchor == null) {
      _recursiveAnchor = schema;
    }

    // Lock
    final Schema recursiveAnchor = _recursiveAnchor;

    if ("#".equals(schema.get("$recursiveRef"))) {
      assert schema.containsKey("__absolute_recursive_ref__");
      final Schema refSchema =
        recursiveAnchor == null
          ? lookup.get(schema.<String>get("__absolute_recursive_ref__"))
          : recursiveAnchor;
      final String keywordLocation = schemaLocation + "/$recursiveRef";
      final OutputUnit result = validate(
        instance,
        recursiveAnchor == null ? schema : recursiveAnchor,
        refSchema,
        instanceLocation,
        keywordLocation,
        evaluated
      );
      if (!result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, "$recursiveRef", keywordLocation, "A sub-schema had errors"));
        errors.addAll(result.getErrors());
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
        throw new IllegalStateException(message);
      }

      final Schema refSchema = lookup.get(uri);
      final String keywordLocation = schemaLocation + "/" + schema.<String>get("$ref");
      final OutputUnit result = validate(
        instance,
        refSchema,
        recursiveAnchor,
        instanceLocation,
        keywordLocation,
        evaluated
      );
      if (!result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, "$ref", keywordLocation, "A subschema had errors"));
        errors.addAll(result.getErrors());
      }
      if (draft == Draft.DRAFT4 || draft == Draft.DRAFT7) {
        return new OutputUnit(errors.isEmpty()).setErrors(errors);
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
        errors.add(new OutputUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + String.join(", ", type.getList())));
      }
    } else if ("integer".equals(schema.get("type"))) {
      if (!"number".equals(instanceType) || !Numbers.isInteger(instance)) {
        errors.add(new OutputUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type")));
      }
    } else if (schema.containsKey("type") && !instanceType.equals(schema.get("type"))) {
      errors.add(new OutputUnit(instanceLocation, "type", schemaLocation + "/type", "Instance type " + instanceType + " is invalid. Expected " + schema.get("type")));
    }

    if (schema.containsKey("const")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (!JSON.deepCompare(instance, schema.get("const"))) {
          errors.add(new OutputUnit(instanceLocation, "const", schemaLocation + "/const", "Instance does not match " + Json.encode(schema.get("const"))));
        }
      } else if (!Utils.Objects.equals(schema.get("const"), instance)) {
        errors.add(new OutputUnit(instanceLocation, "const", schemaLocation + "/const", "Instance does not match " + Json.encode(schema.get("const"))));
      }
    }

    if (schema.containsKey("enum")) {
      if ("object".equals(instanceType) || "array".equals(instanceType)) {
        if (schema.<JsonArray>get("enum").stream().noneMatch(value -> JSON.deepCompare(instance, value))) {
          errors.add(new OutputUnit(instanceLocation, "enum", schemaLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum"))));
        }
      } else if (schema.<JsonArray>get("enum").stream().noneMatch(value -> Utils.Objects.equals(instance, value))) {
        errors.add(new OutputUnit(instanceLocation, "enum", schemaLocation + "/enum", "Instance does not match any of " + Json.encode(schema.get("enum"))));
      }
    }

    if (schema.containsKey("not")) {
      final String keywordLocation = schemaLocation + "/not";
      final OutputUnit result = validate(
        instance,
        Schemas.wrap((JsonObject) schema, "not"),
        recursiveAnchor,
        instanceLocation,
        keywordLocation,
        new HashSet<>()
      );
      if (result.getValid()) {
        errors.add(new OutputUnit(instanceLocation, "not", keywordLocation, "Instance matched \"not\" schema"));
      }
    }

    Set<Object> subEvaluateds = new HashSet<>();

    if (schema.containsKey("anyOf")) {
      final String keywordLocation = schemaLocation + "/anyOf";
      final int errorsLength = errors.size();
      boolean anyValid = false;
      for (int i = 0; i < schema.<JsonArray>get("anyOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("anyOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.getErrors());
        anyValid = anyValid || result.getValid();
        if (result.getValid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (anyValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new OutputUnit(instanceLocation, "anyOf", keywordLocation, "Instance does not match any subschemas"));
      }
    }

    if (schema.containsKey("allOf")) {
      final String keywordLocation = schemaLocation + "/allOf";
      final int errorsLength = errors.size();
      boolean allValid = true;
      for (int i = 0; i < schema.<JsonArray>get("allOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("allOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.getErrors());
        allValid = allValid && result.getValid();
        if (result.getValid()) {
          subEvaluateds.addAll(subEvaluated);
        }
      }
      if (allValid) {
        errors = errors.subList(0, Math.min(errors.size(), errorsLength));
      } else {
        errors.add(errorsLength, new OutputUnit(instanceLocation, "allOf", keywordLocation, "Instance does not match every subschema"));
      }
    }

    if (schema.containsKey("oneOf")) {
      final String keywordLocation = schemaLocation + "/oneOf";
      final int errorsLength = errors.size();
      int matches = 0;
      for (int i = 0; i < schema.<JsonArray>get("oneOf").size(); i++) {
        final Set<Object> subEvaluated = new HashSet<>(evaluated);
        final OutputUnit result = validate(
          instance,
          Schemas.wrap(schema.get("oneOf"), i),
          schema.<Boolean>get("$recursiveAnchor", false) ? recursiveAnchor : null,
          instanceLocation,
          keywordLocation + "/" + i,
          subEvaluated
        );
        errors.addAll(result.getErrors());
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
        errors.add(errorsLength, new OutputUnit(instanceLocation, "oneOf", keywordLocation, "Instance does not match exactly one subschema (" + matches + " matches)"));
      }
    }

    if ("object".equals(instanceType) || "array".equals(instanceType)) {
      evaluated.addAll(subEvaluateds);
    }

    if (schema.containsKey("if")) {
      final String keywordLocation = schemaLocation + "/if";
      final OutputUnit conditionResult = validate(
        instance,
        Schemas.wrap((JsonObject) schema, "if"),
        recursiveAnchor,
        instanceLocation,
        keywordLocation,
        evaluated
      );
      if (conditionResult.getValid()) {
        if (schema.containsKey("then")) {
          final OutputUnit thenResult = validate(
            instance,
            Schemas.wrap((JsonObject) schema, "then"),
            recursiveAnchor,
            instanceLocation,
            schemaLocation + "/then",
            evaluated
          );
          if (!thenResult.getValid()) {
            errors.add(new OutputUnit(instanceLocation, "if", keywordLocation, "Instance does not match \"then\" schema"));
            errors.addAll(thenResult.getErrors());
          }
        }
      } else if (schema.containsKey("else")) {
        final OutputUnit elseResult = validate(
          instance,
          Schemas.wrap((JsonObject) schema, "else"),
          recursiveAnchor,
          instanceLocation,
          schemaLocation + "/else",
          evaluated
        );
        if (!elseResult.getValid()) {
          errors.add(new OutputUnit(instanceLocation, "if", keywordLocation, "Instance does not match \"else\" schema"));
          errors.addAll(elseResult.getErrors());
        }
      }
    }

    switch (instanceType) {
      case "object": {
        if (schema.containsKey("required")) {
          for (final Object key : schema.<JsonArray>get("required")) {
            if (!((JsonObject) instance).containsKey((String) key)) {
              errors.add(new OutputUnit(instanceLocation, "required", schemaLocation + "/required", "Instance does not have required property \"" + key + "\""));
            }
          }
        }

        final Set<String> keys = ((JsonObject) instance).fieldNames();

        if (schema.containsKey("minProperties") && keys.size() < schema.<Integer>get("minProperties")) {
          errors.add(new OutputUnit(instanceLocation, "minProperties", schemaLocation + "/minProperties", "Instance does not have at least " + schema.get("minProperties") + " properties"));
        }

        if (schema.containsKey("maxProperties") && keys.size() > schema.<Integer>get("maxProperties")) {
          errors.add(new OutputUnit(instanceLocation, "maxProperties", schemaLocation + "/maxProperties", "Instance does not have at least " + schema.get("maxProperties") + " properties"));
        }

        if (schema.containsKey("propertyNames")) {
          final String keywordLocation = schemaLocation + "/propertyNames";
          for (final String key : ((JsonObject) instance).fieldNames()) {
            final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
            final OutputUnit result = validate(
              key,
              Schemas.wrap((JsonObject) schema, "propertyNames"),
              recursiveAnchor,
              subInstancePointer,
              keywordLocation,
              new HashSet<>()
            );
            if (!result.getValid()) {
              errors.add(new OutputUnit(instanceLocation, "propertyNames", keywordLocation, "Property name \"" + key + "\" does not match schema"));
              errors.addAll(result.getErrors());
            }
          }
        }

        if (schema.containsKey("dependentRequired")) {
          final String keywordLocation = schemaLocation + "/dependantRequired";
          for (final String key : schema.<JsonObject>get("dependentRequired").fieldNames()) {
            if (((JsonObject) instance).containsKey(key)) {
              final JsonArray required = schema.<JsonObject>get("dependentRequired").getJsonArray(key);
              for (final Object dependantKey : required) {
                if (!(((JsonObject) instance).containsKey((String) dependantKey))) {
                  errors.add(new OutputUnit(instanceLocation, "dependentRequired", keywordLocation, "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\""));
                }
              }
            }
          }
        }

        if (schema.containsKey("dependentSchemas")) {
          for (final String key : schema.<JsonObject>get("dependentSchemas").fieldNames()) {
            final String keywordLocation = schemaLocation + "/dependentSchemas";
            if (((JsonObject) instance).containsKey(key)) {
              final OutputUnit result = validate(
                instance,
                Schemas.wrap(schema.get("dependentSchemas"), key),
                recursiveAnchor,
                instanceLocation,
                keywordLocation + "/" + Pointers.encode(key),
                evaluated
              );
              if (!result.getValid()) {
                errors.add(new OutputUnit(instanceLocation, "dependentSchemas", keywordLocation, "Instance has \"" + key + "\" but does not match dependant schema"));
                errors.addAll(result.getErrors());
              }
            }
          }
        }

        if (schema.containsKey("dependencies")) {
          final String keywordLocation = schemaLocation + "/dependencies";
          for (final String key : schema.<JsonObject>get("dependencies").fieldNames()) {
            if (((JsonObject) instance).containsKey(key)) {
              final Object propsOrSchema = schema.<JsonObject>get("dependencies").getValue(key);
              if (propsOrSchema instanceof JsonArray) {
                for (final Object dependantKey : ((JsonArray) propsOrSchema)) {
                  if (!((JsonObject) instance).containsKey((String) dependantKey)) {
                    errors.add(new OutputUnit(instanceLocation, "dependencies", keywordLocation, "Instance has \"" + key + "\" but does not have \"" + dependantKey + "\""));
                  }
                }
              } else {
                final OutputUnit result = validate(
                  instance,
                  Schemas.wrap(schema.get("dependencies"), key),
                  recursiveAnchor,
                  instanceLocation,
                  keywordLocation + "/" + Pointers.encode(key),
                  new HashSet<>()
                );
                if (!result.getValid()) {
                  errors.add(new OutputUnit(instanceLocation, "dependencies", keywordLocation, "Instance has \"" + key + "\" but does not match dependant schema"));
                  errors.addAll(result.getErrors());
                }
              }
            }
          }
        }

        final Set<Object> thisEvaluated = new HashSet<>();

        boolean stop = false;

        if (schema.containsKey("properties")) {
          final String keywordLocation = schemaLocation + "/properties";
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
              keywordLocation + "/" + Pointers.encode(key),
              new HashSet<>()
            );
            if (result.getValid()) {
              evaluated.add(key);
              thisEvaluated.add(key);
            } else {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(instanceLocation, "properties", keywordLocation, "Property \"" + key + "\" does not match schema"));
              errors.addAll(result.getErrors());
              if (stop) {
                break;
              }
            }
          }
        }

        if (!stop && schema.containsKey("patternProperties")) {
          final String keywordLocation = schemaLocation + "/patternProperties";
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
                keywordLocation + "/" + Pointers.encode(pattern),
                new HashSet<>()
              );
              if (result.getValid()) {
                evaluated.add(key);
                thisEvaluated.add(key);
              } else {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, "patternProperties", keywordLocation, "Property \"" + key + "\" matches pattern \"" + pattern + "\" but does not match associated schema"));
                errors.addAll(result.getErrors());
              }
            }
          }
        }

        if (!stop && schema.containsKey("additionalProperties")) {
          final String keywordLocation = schemaLocation + "/additionalProperties";
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
              keywordLocation,
              new HashSet<>()
            );
            if (result.getValid()) {
              evaluated.add(key);
            } else {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(instanceLocation, "additionalProperties", keywordLocation, "Property \"" + key + "\" does not match additional properties schema"));
              errors.addAll(result.getErrors());
            }
          }
        } else if (!stop && schema.containsKey("unevaluatedProperties")) {
          final String keywordLocation = schemaLocation + "/unevaluatedProperties";
          for (final String key : ((JsonObject) instance).fieldNames()) {
            if (!evaluated.contains(key)) {
              final String subInstancePointer = instanceLocation + "/" + Pointers.encode(key);
              final OutputUnit result = validate(
                ((JsonObject) instance).getValue(key),
                Schemas.wrap((JsonObject) schema, "unevaluatedProperties"),
                recursiveAnchor,
                subInstancePointer,
                keywordLocation,
                new HashSet<>()
              );
              if (result.getValid()) {
                evaluated.add(key);
              } else {
                errors.add(new OutputUnit(instanceLocation, "unevaluatedProperties", keywordLocation, "Property \"" + key + "\" does not match unevaluated properties schema"));
                errors.addAll(result.getErrors());
              }
            }
          }
        }
        break;
      }
      case "array": {
        if (schema.containsKey("maxItems") && ((JsonArray) instance).size() > schema.<Integer>get("maxItems")) {
          errors.add(new OutputUnit(instanceLocation, "maxItems", schemaLocation + "/maxItems", "Array has too many items ( + " + ((JsonArray) instance).size() + " > " + schema.get("maxItems") + ")"));
        }

        if (schema.containsKey("minItems") && ((JsonArray) instance).size() < schema.<Integer>get("minItems")) {
          errors.add(new OutputUnit(instanceLocation, "minItems", schemaLocation + "/minItems", "Array has too few items ( + " + ((JsonArray) instance).size() + " < " + schema.get("minItems") + ")"));
        }

        final int length = ((JsonArray) instance).size();
        int i = 0;
        boolean stop = false;

        if (schema.containsKey("prefixItems")) {
          final String keywordLocation = schemaLocation + "/prefixItems";
          final int length2 = Math.min(schema.<JsonArray>get("prefixItems").size(), length);
          for (; i < length2; i++) {
            final OutputUnit result = validate(
              ((JsonArray) instance).getValue(i),
              Schemas.wrap(schema.get("prefixItems"), i),
              recursiveAnchor,
              instanceLocation + "/" + i,
              keywordLocation + "/" + i,
              new HashSet<>()
            );
            evaluated.add(i);
            if (!result.getValid()) {
              stop = outputFormat == OutputFormat.Flag;
              errors.add(new OutputUnit(instanceLocation, "prefixItems", keywordLocation, "Items did not match schema"));
              errors.addAll(result.getErrors());
              if (stop) {
                break;
              }
            }
          }
        }

        if (schema.containsKey("items")) {
          final String keywordLocation = schemaLocation + "/items";
          if (schema.get("items") instanceof JsonArray) {
            final int length2 = Math.min(schema.<JsonArray>get("items").size(), length);
            for (; i < length2; i++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(i),
                Schemas.wrap(schema.get("items"), i),
                recursiveAnchor,
                instanceLocation + "/" + i,
                keywordLocation + "/" + i,
                new HashSet<>()
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, "items", keywordLocation, "Items did not match schema"));
                errors.addAll(result.getErrors());
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
                keywordLocation,
                new HashSet<>()
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, "items", keywordLocation, "Items did not match schema"));
                errors.addAll(result.getErrors());
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
                new HashSet<>()
              );
              evaluated.add(i);
              if (!result.getValid()) {
                stop = outputFormat == OutputFormat.Flag;
                errors.add(new OutputUnit(instanceLocation, "additionalItems", keywordLocation2, "Items did not match additional items schema"));
                errors.addAll(result.getErrors());
              }
            }
          }
        }

        if (schema.containsKey("contains")) {
          if (length == 0 && !schema.containsKey("minContains")) {
            errors.add(new OutputUnit(instanceLocation, "contains", schemaLocation + "/contains", "Array is empty. It must contain at least one item matching the schema"));
          } else if (schema.containsKey("minContains") && length < schema.<Integer>get("minContains")) {
            errors.add(new OutputUnit(instanceLocation, "minContains", schemaLocation + "/minContains", "Array has less items (" + length + ") than minContains (" + schema.get("minContains") + ")"));
          } else {
            final String keywordLocation = schemaLocation + "/contains";
            final int errorsLength = errors.size();
            int contained = 0;
            for (int j = 0; j < length; j++) {
              final OutputUnit result = validate(
                ((JsonArray) instance).getValue(j),
                Schemas.wrap((JsonObject) schema, "contains"),
                recursiveAnchor,
                instanceLocation + "/" + i,
                keywordLocation,
                new HashSet<>()
              );
              if (result.getValid()) {
                evaluated.add(j);
                contained++;
              } else {
                errors.addAll(result.getErrors());
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
              errors.add(errorsLength, new OutputUnit(instanceLocation, "contains", keywordLocation, "Array does not contain item matching schema"));
            } else if (schema.containsKey("minContains") && contained < schema.<Integer>get("minContains")) {
              errors.add(new OutputUnit(instanceLocation, "minContains", keywordLocation + "/minContains", "Array must contain at least " + schema.get("minContains") + " items matching schema. Only " + contained + " items were found"));
            } else if (schema.containsKey("maxContains") && contained > schema.<Integer>get("maxContains")) {
              errors.add(new OutputUnit(instanceLocation, "maxContains", keywordLocation + "/maxContains", "Array may contain at most " + schema.get("minContains") + " items matching schema. " + contained + " items were found"));
            }
          }
        }

        if (!stop && schema.containsKey("unevaluatedItems")) {
          final String keywordLocation = schemaLocation + "/unevaluatedItems";
          for (; i < length; i++) {
            if (evaluated.contains(i)) {
              continue;
            }
            final OutputUnit result = validate(
              ((JsonArray) instance).getValue(i),
              Schemas.wrap((JsonObject) schema, "unevaluatedItems"),
              recursiveAnchor,
              instanceLocation + "/" + i,
              keywordLocation,
              new HashSet<>()
            );
            evaluated.add(i);
            if (!result.getValid()) {
              errors.add(new OutputUnit(instanceLocation, "unevaluatedItems", keywordLocation, "Items did not match unevaluated items schema"));
              errors.addAll(result.getErrors());
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
                errors.add(new OutputUnit(instanceLocation, "uniqueItems", schemaLocation + "/uniqueItems", "Duplicate items at indexes " + j + " and " + k));
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
            errors.add(new OutputUnit(instanceLocation, "minimum", schemaLocation + "/minimum", instance + " is less than " + (schema.<Boolean>get("exclusiveMinimum", false) ? "or equal to " : "") + schema.get("minimum")));
          }
          if (
            schema.containsKey("maximum") &&
              ((schema.<Boolean>get("exclusiveMaximum", false) && Numbers.gte((Number) instance, schema.get("maximum"))) ||
                Numbers.gt((Number) instance, schema.get("maximum")))
          ) {
            errors.add(new OutputUnit(instanceLocation, "maximum", schemaLocation + "/maximum", instance + " is greater than " + (schema.<Boolean>get("exclusiveMaximum", false) ? "or equal to " : "") + schema.get("maximum")));
          }
        } else {
          if (schema.containsKey("minimum") && Numbers.lt((Number) instance, schema.get("minimum"))) {
            errors.add(new OutputUnit(instanceLocation, "minimum", schemaLocation + "/minimum", instance + " is less than " + schema.get("minimum")));
          }
          if (schema.containsKey("maximum") && Numbers.gt((Number) instance, schema.get("maximum"))) {
            errors.add(new OutputUnit(instanceLocation, "maximum", schemaLocation + "/maximum", instance + " is greater than " + schema.get("maximum")));
          }
          if (schema.containsKey("exclusiveMinimum") && Numbers.lte((Number) instance, schema.get("exclusiveMinimum"))) {
            errors.add(new OutputUnit(instanceLocation, "exclusiveMinimum", schemaLocation + "/exclusiveMinimum", instance + " is less than or equal to " + schema.get("exclusiveMinimum")));
          }
          if (schema.containsKey("exclusiveMaximum") && Numbers.gte((Number) instance, schema.get("exclusiveMaximum"))) {
            errors.add(new OutputUnit(instanceLocation, "exclusiveMaximum", schemaLocation + "/exclusiveMaximum", instance + " is greater than or equal to " + schema.get("exclusiveMaximum")));
          }
        }
        if (schema.containsKey("multipleOf")) {
          final double remainder = Numbers.remainder((Number) instance, schema.get("multipleOf"));
          if (
            Math.abs(0 - remainder) >= 1.1920929e-7 &&
              Math.abs(schema.<Number>get("multipleOf").doubleValue() - remainder) >= 1.1920929e-7
          ) {
            errors.add(new OutputUnit(instanceLocation, "multipleOf", schemaLocation + "/multipleOf", instance + " is not a multiple of " + schema.get("multipleOf")));
          }
        }
        break;
      case "string": {
        final int length =
          !schema.containsKey("minLength") && !schema.containsKey("maxLength")
            ? 0
            : Strings.ucs2length((String) instance);
        if (schema.containsKey("minLength") && Numbers.lt(length, schema.get("minLength"))) {
          errors.add(new OutputUnit(instanceLocation, "minLength", schemaLocation + "/minLength", "String is too short (" + length + " < " + schema.get("minLength") + ")"));
        }
        if (schema.containsKey("maxLength") && Numbers.gt(length, schema.get("maxLength"))) {
          errors.add(new OutputUnit(instanceLocation, "maxLength", schemaLocation + "/maxLength", "String is too long (" + length + " > " + schema.get("maxLength") + ")"));
        }
        if (schema.containsKey("pattern") && !Pattern.compile(schema.get("pattern")).matcher((String) instance).find()) {
          errors.add(new OutputUnit(instanceLocation, "pattern", schemaLocation + "/pattern", "String does not match pattern"));
        }
        if (
          schema.containsKey("format") &&
            !Format.fastFormat(schema.get("format"), (String) instance)
        ) {
          errors.add(new OutputUnit(instanceLocation, "format", schemaLocation + "/format", "String does not match format \"" + schema.get("format") + "\""));
        }
        break;
      }
    }

    return new OutputUnit(errors.isEmpty()).setErrors(errors);
  }
}
