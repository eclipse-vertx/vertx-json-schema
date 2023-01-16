package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.json.schema.draft7.dsl.StringFormat;

import static io.vertx.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.json.schema.draft7.dsl.Schemas.*;

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
