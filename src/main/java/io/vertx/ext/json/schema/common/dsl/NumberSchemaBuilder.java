package io.vertx.ext.json.schema.common.dsl;

import io.vertx.codegen.annotations.Fluent;

public final class NumberSchemaBuilder extends SchemaBuilder<NumberSchemaBuilder, NumberKeyword> {

    NumberSchemaBuilder() {
        super(SchemaType.NUMBER);
    }

    @Fluent
    public NumberSchemaBuilder asInteger() {
        type(SchemaType.INT);
        return this;
    }

    public boolean isIntegerSchema() {
        return this.type.equals(SchemaType.INT);
    }

}
