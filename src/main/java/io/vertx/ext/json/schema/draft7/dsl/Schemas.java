package io.vertx.ext.json.schema.draft7.dsl;

import io.vertx.ext.json.schema.common.dsl.GenericSchemaBuilder;
import io.vertx.ext.json.schema.common.dsl.Keyword;
import io.vertx.ext.json.schema.common.dsl.SchemaBuilder;

import java.util.Objects;

public class Schemas extends io.vertx.ext.json.schema.common.dsl.Schemas {

  public static GenericSchemaBuilder ifThenElse(SchemaBuilder ifSchema, SchemaBuilder thenSchema, SchemaBuilder elseSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(thenSchema);
    Objects.requireNonNull(elseSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("then", thenSchema::toJson),
        new Keyword("else", elseSchema::toJson)
      );
  }

  public static GenericSchemaBuilder ifThen(SchemaBuilder ifSchema, SchemaBuilder thenSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(thenSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("then", thenSchema::toJson)
      );
  }

  public static GenericSchemaBuilder ifElse(SchemaBuilder ifSchema, SchemaBuilder elseSchema) {
    Objects.requireNonNull(ifSchema);
    Objects.requireNonNull(elseSchema);
    return new GenericSchemaBuilder()
      .with(
        new Keyword("if", ifSchema::toJson),
        new Keyword("else", elseSchema::toJson)
      );
  }

}
