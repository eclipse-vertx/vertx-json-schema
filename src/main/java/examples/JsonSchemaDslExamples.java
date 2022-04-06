package examples;

import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.json.schema.draft7.dsl.StringFormat;

import static io.vertx.json.schema.common.dsl.Schemas.*;
import static io.vertx.json.schema.draft7.dsl.Keywords.*;

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
      .with(type(SchemaType.ARRAY, SchemaType.INT));
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

  public void parse(SchemaParser parser) {
    Schema schema = objectSchema()
      .requiredProperty("name", stringSchema())
      .requiredProperty("age", intSchema())
      .additionalProperties(stringSchema())
      .build(parser);
  }

}
