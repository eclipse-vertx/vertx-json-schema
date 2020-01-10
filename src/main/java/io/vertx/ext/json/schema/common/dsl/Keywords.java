package io.vertx.ext.json.schema.common.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Keywords {

    public static Keyword type(SchemaType... types) {
        Objects.requireNonNull(types);
        if (types.length == 1)
            return new Keyword("type", types[0].getName());
        else
            return new Keyword("type", Arrays.stream(types).map(SchemaType::getName).collect(Collectors.toList()));
    }

    public static NumberKeyword multipleOf(double multipleOf) {
        return new NumberKeyword("multipleOf", multipleOf);
    }

    public static StringKeyword maxLength(int maxLength) { return new StringKeyword("maxLength", maxLength); }

    public static StringKeyword minLength(int minLength) { return new StringKeyword("minLength", minLength); }

    public static StringKeyword pattern(Pattern pattern) { 
        Objects.requireNonNull(pattern);
        return new StringKeyword("pattern", pattern.toString());
    }

    public static ArrayKeyword maxItems(int maxItems) { return new ArrayKeyword("maxItems", maxItems); }

    public static ArrayKeyword minItems(int minItems) { return new ArrayKeyword("minItems", minItems); }

    public static ArrayKeyword uniqueItems() { return new ArrayKeyword("uniqueItems", true); }
    
    public static ObjectKeyword maxProperties(int maxProperties) { return new ObjectKeyword("maxProperties", maxProperties); }

    public static ObjectKeyword minProperties(int minProperties) { return new ObjectKeyword("minProperties", minProperties); }

}
