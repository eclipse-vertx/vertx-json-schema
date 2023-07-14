package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.json.schema.common.dsl.StringFormat;

import static io.vertx.json.schema.common.dsl.Keywords.format;
import static io.vertx.json.schema.common.dsl.Keywords.maxItems;
import static io.vertx.json.schema.common.dsl.Keywords.type;
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.refToAlias;
import static io.vertx.json.schema.common.dsl.Schemas.schema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.json.schema.common.dsl.Schemas.tupleSchema;

public class JsonSchemaDslExamples {

  public void createSchema() {
    SchemaBuilder intSchemaBuilder = intSchema();
    SchemaBuilder objectSchemaBuilder = objectSchema();
  }

  public void keywords() {
    stringSchema()
      .with(format(StringFormat.DATETIME));
    arraySchema()
      .with(maxItems(10));
    schema() // Generic schema that accepts both arrays and integers
      .with(type(SchemaType.ARRAY, SchemaType.INTEGER));
  }

  public void createObject() {
    objectSchema()
      .requiredProperty("name", stringSchema())
      .requiredProperty("age", intSchema())
      .additionalProperties(stringSchema());
  }

  public void createArray() {
    arraySchema()
      .items(stringSchema());
  }

  public void createTuple() {
    tupleSchema()
      .item(stringSchema()) // First item
      .item(intSchema()) // Second item
      .item(booleanSchema()); // Third item
  }

  public void alias() {
    intSchema()
      .alias("myInt");

    objectSchema()
      .requiredProperty("anInteger", refToAlias("myInt"));
  }

  public void parse() {
    JsonObject schema = objectSchema()
      .requiredProperty("name", stringSchema())
      .requiredProperty("age", intSchema())
      .additionalProperties(stringSchema())
      .toJson();
  }

}
