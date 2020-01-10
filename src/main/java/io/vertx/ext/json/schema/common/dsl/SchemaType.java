package io.vertx.ext.json.schema.common.dsl;

public enum SchemaType {
    INT("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    STRING("string"),
    ARRAY("array"),
    OBJECT("object");

    private String name;

    SchemaType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
