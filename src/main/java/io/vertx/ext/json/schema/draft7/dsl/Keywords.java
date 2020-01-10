package io.vertx.ext.json.schema.draft7.dsl;

import io.vertx.ext.json.schema.common.dsl.*;

import java.util.Objects;

public class Keywords extends io.vertx.ext.json.schema.common.dsl.Keywords {

  public static NumberKeyword exclusiveMaximum(double exclusiveMaximum) {
    return new NumberKeyword("exclusiveMaximum", exclusiveMaximum);
  }

  public static NumberKeyword maximum(double maximum) {
    return new NumberKeyword("maximum", maximum);
  }

  public static NumberKeyword exclusiveMinimum(double exclusiveMinimum) {
    return new NumberKeyword("exclusiveMinimum", exclusiveMinimum);
  }

  public static NumberKeyword minimum(double minimum) {
    return new NumberKeyword("minimum", minimum);
  }

  public static StringKeyword format(StringFormat format) {
    Objects.requireNonNull(format);
    return new StringKeyword("format", format.getName());
  }

  public static ArrayKeyword contains(SchemaBuilder builder) {
    Objects.requireNonNull(builder);
    return new ArrayKeyword("contains", builder::toJson);
  }

  public static ObjectKeyword propertyNames(StringSchemaBuilder schemaBuilder) {
    Objects.requireNonNull(schemaBuilder);
    return new ObjectKeyword("propertyNames", schemaBuilder::toJson);
  }

}
