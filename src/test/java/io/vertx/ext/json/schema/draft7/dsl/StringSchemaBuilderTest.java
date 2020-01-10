package io.vertx.ext.json.schema.draft7.dsl;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.vertx.ext.json.schema.TestUtils.entry;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.stringSchema;

public class StringSchemaBuilderTest {

  @Test
  public void testKeywords(){
    assertThat(
        stringSchema()
          .with(maxLength(10), minLength(1), pattern(Pattern.compile("[a-zA-Z]*")), format(StringFormat.REGEX))
        .toJson()
    )   .removingEntry("$id")
        .containsAllAndOnlyEntries(
            entry("type", "string"),
            entry("maxLength", 10),
            entry("minLength", 1),
            entry("pattern", Pattern.compile("[a-zA-Z]*").toString()),
            entry("format", "regex")
        );
  }

}
