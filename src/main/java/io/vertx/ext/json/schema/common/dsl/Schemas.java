package io.vertx.ext.json.schema.common.dsl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.common.SchemaURNId;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class Schemas {

  /**
   * Creates a generic untyped schema. You can add the type keyword using {@link Keywords#type(SchemaType...)}
   *
   * @return
   */
  public static GenericSchemaBuilder schema() {
    return new GenericSchemaBuilder();
  }

  public static NumberSchemaBuilder intSchema() {
    return new NumberSchemaBuilder().asInteger();
  }

  public static NumberSchemaBuilder numberSchema() {
    return new NumberSchemaBuilder();
  }

  public static StringSchemaBuilder stringSchema() {
    return new StringSchemaBuilder();
  }

  public static BooleanSchemaBuilder booleanSchema() {
    return new BooleanSchemaBuilder();
  }

  public static TupleSchemaBuilder tupleSchema() {
    return new TupleSchemaBuilder();
  }

  public static ArraySchemaBuilder arraySchema() {
    return new ArraySchemaBuilder();
  }

  public static ObjectSchemaBuilder objectSchema() {
    return new ObjectSchemaBuilder();
  }

  public static GenericSchemaBuilder constSchema(@Nullable Object constValue) {
    return new GenericSchemaBuilder().with(new Keyword("const", constValue));
  }

  public static GenericSchemaBuilder enumSchema(@Nullable Object... enumValues) {
    return new GenericSchemaBuilder().with(new Keyword("enum", Arrays.asList(enumValues)));
  }

  public static GenericSchemaBuilder ref(JsonPointer pointer) {
    Objects.requireNonNull(pointer);
    return new GenericSchemaBuilder().with(new Keyword("$ref", pointer.toURI().toString()));
  }

  public static GenericSchemaBuilder refToAlias(String alias) {
    Objects.requireNonNull(alias);
    return ref(new SchemaURNId(alias).toPointer());
  }

  public static GenericSchemaBuilder allOf(SchemaBuilder... allOf) {
    Objects.requireNonNull(allOf);
    return new GenericSchemaBuilder().with(new Keyword("allOf",
      collectSchemaBuilders(allOf)
    ));
  }

  public static GenericSchemaBuilder anyOf(SchemaBuilder... anyOf) {
    Objects.requireNonNull(anyOf);
    return new GenericSchemaBuilder().with(new Keyword("anyOf",
      collectSchemaBuilders(anyOf)
    ));
  }

  public static GenericSchemaBuilder oneOf(SchemaBuilder... oneOf) {
    Objects.requireNonNull(oneOf);
    return new GenericSchemaBuilder().with(new Keyword("oneOf",
      collectSchemaBuilders(oneOf)
    ));
  }

  public static GenericSchemaBuilder not(SchemaBuilder not) {
    Objects.requireNonNull(not);
    return new GenericSchemaBuilder().with(new Keyword("not", not::toJson));
  }

  private static Supplier<Object> collectSchemaBuilders(SchemaBuilder... schemaBuilders) {
    return () -> Arrays.stream(schemaBuilders).collect(
      Collector.of(JsonArray::new, (j, b) -> j.add(b.toJson()), JsonArray::addAll)
    );
  }

}
