package io.vertx.ext.json.schema.common.dsl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.regex.Pattern;

public final class ObjectSchemaBuilder extends SchemaBuilder<ObjectSchemaBuilder, ObjectKeyword> {

  Map<String, SchemaBuilder> properties;
  Map<Pattern, SchemaBuilder> patternProperties;
  Set<String> requiredProperties;

  ObjectSchemaBuilder() {
    super(SchemaType.OBJECT);
    this.properties = new HashMap<>();
    this.requiredProperties = new HashSet<>();
    this.patternProperties = new HashMap<>();
  }

  public ObjectSchemaBuilder property(String name, SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(schemaBuilder);
    properties.put(name, schemaBuilder);
    return this;
  }

  public ObjectSchemaBuilder optionalProperty(String name, SchemaBuilder schemaBuilder) {
    return property(name, schemaBuilder);
  }

  public Map<String, SchemaBuilder> getProperties() {
    return properties;
  }

  public Map<Pattern, SchemaBuilder> getPatternProperties() {
    return patternProperties;
  }

  public boolean isPropertyRequired(String property) {
    return this.requiredProperties.contains(property);
  }

  public ObjectSchemaBuilder requiredProperty(String name, SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(schemaBuilder);
    this.requiredProperties.add(name);
    return this.property(name, schemaBuilder);
  }

  public ObjectSchemaBuilder patternProperty(Pattern pattern, SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(pattern);
    Objects.requireNonNull(schemaBuilder);
    this.patternProperties.put(pattern, schemaBuilder);
    return this;
  }

  public ObjectSchemaBuilder additionalProperties(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.keywords.put("additionalProperties", schemaBuilder::toJson);
    return this;
  }

  public ObjectSchemaBuilder allowAdditionalProperties(boolean allow) {
    this.keywords.put("additionalProperties", () -> allow);
    return this;
  }

  @Override
  public JsonObject toJson() {
    if (!properties.isEmpty())
      this.keywords.put("properties", () ->
          properties
              .entrySet()
              .stream()
              .collect(JsonObject::new, (jo, e) -> jo.put(e.getKey(), e.getValue().toJson()), JsonObject::mergeIn)
      );
    if (!requiredProperties.isEmpty())
      this.keywords.put("required", () -> new JsonArray(new ArrayList<>(requiredProperties)));
    if (!patternProperties.isEmpty())
      this.keywords.put("patternProperties", () ->
          patternProperties
              .entrySet()
              .stream()
              .collect(JsonObject::new, (jo, e) -> jo.put(e.getKey().toString(), e.getValue().toJson()), JsonObject::mergeIn)
      );
    return super.toJson();
  }
}
