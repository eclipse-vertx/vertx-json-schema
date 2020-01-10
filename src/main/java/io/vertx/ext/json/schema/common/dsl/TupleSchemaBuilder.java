package io.vertx.ext.json.schema.common.dsl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class TupleSchemaBuilder extends SchemaBuilder<TupleSchemaBuilder, ArrayKeyword> {

  // For items keyword as list of schemas
  private List<SchemaBuilder> itemList;
  private SchemaBuilder additionalItems;

  TupleSchemaBuilder() {
    super(SchemaType.ARRAY);
    this.itemList = new LinkedList<>();
  }

  @Fluent
  public TupleSchemaBuilder item(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.itemList.add(schemaBuilder);
    return this;
  }

  @Fluent
  public TupleSchemaBuilder additionalItems(SchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    this.additionalItems = schemaBuilder;
    return this;
  }

  public List<SchemaBuilder> getItemList() {
    return itemList;
  }

  public SchemaBuilder getAdditionalItems() {
    return additionalItems;
  }

  @Override
  public JsonObject toJson() {
    if (!itemList.isEmpty())
      this.keywords.put("items", () -> itemList.stream().collect(JsonArray::new, (ja, s) -> ja.add(s.toJson()), JsonArray::addAll));
    if (additionalItems != null)
      this.keywords.put("additionalItems", additionalItems::toJson);
    return super.toJson();
  }
}
