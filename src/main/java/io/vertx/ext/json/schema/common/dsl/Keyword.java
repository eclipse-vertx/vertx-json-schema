package io.vertx.ext.json.schema.common.dsl;

import java.util.function.Supplier;

public class Keyword {

    private String keyword;
    private Supplier<Object> value;

    public Keyword(String keyword, Supplier<Object> value) {
        this.keyword = keyword;
        this.value = value;
    }

    public Keyword(String keyword, Object value) {
        this.keyword = keyword;
        this.value = () -> value;
    }

    public String getKeyword() {
        return keyword;
    }

    public Supplier<Object> getValueSupplier() {
        return value;
    }
}
