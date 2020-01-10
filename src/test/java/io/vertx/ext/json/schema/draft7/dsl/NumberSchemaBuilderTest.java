package io.vertx.ext.json.schema.draft7.dsl;

import org.junit.jupiter.api.Test;

import static io.vertx.ext.json.schema.TestUtils.entry;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static io.vertx.ext.json.schema.draft7.dsl.Keywords.*;
import static io.vertx.ext.json.schema.draft7.dsl.Schemas.numberSchema;

public class NumberSchemaBuilderTest {

  @Test
  public void testNumberSchema(){
    assertThat(
        numberSchema().toJson()
    )   .removingEntry("$id")
        .containsAllAndOnlyEntries(entry("type", "number"));
  }

  @Test
  public void testIntegerSchema(){
    assertThat(
        numberSchema().asInteger().toJson()
    )   .removingEntry("$id")
        .containsAllAndOnlyEntries(entry("type", "integer"));
  }

  @Test
  public void testKeywords(){
    assertThat(
        numberSchema()
            .with(multipleOf(2d), exclusiveMaximum(10d), maximum(10d), exclusiveMinimum(10d), minimum(10d))
            .toJson()
    )   .removingEntry("$id")
        .containsAllAndOnlyEntries(
            entry("type", "number"),
            entry("multipleOf", 2d),
            entry("exclusiveMaximum", 10d),
            entry("exclusiveMinimum", 10d),
            entry("maximum", 10d),
            entry("minimum", 10d)
        );
  }

}
