package io.vertx.ext.json.schema.common.dsl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public final class ArraySchemaBuilder extends SchemaBuilder<ArraySchemaBuilder, ArrayKeyword> {

  ArraySchemaBuilder() {
    super(SchemaType.ARRAY);
  }

  @Fluent
  public ArraySchemaBuilder items(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.keywords.put("items", schemaBuilder::toJson);
    return this;
  }

  @Override
  public JsonObject toJson() {
    return super.toJson();
  }
}
