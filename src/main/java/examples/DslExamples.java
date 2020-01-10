package examples;

import io.vertx.docgen.Source;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaParser;
import io.vertx.ext.json.schema.common.dsl.SchemaBuilder;
import io.vertx.ext.json.schema.common.dsl.SchemaType;
import io.vertx.ext.json.schema.draft7.dsl.StringFormat;

import static io.vertx.ext.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.*;

@Source
public class DslExamples {

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
