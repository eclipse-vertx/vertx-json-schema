package io.vertx.tests;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.Validator;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class FormatValidationTest {

  private static final JsonObject DATE_SCHEMA = new JsonObject()
    .put("type", "string")
    .put("format", "date");

  private static Validator validator(Draft draft, Boolean formatValidation) {
    JsonSchemaOptions options = new JsonSchemaOptions()
      .setBaseUri("https://vertx.io")
      .setDraft(draft);
    if (formatValidation != null) {
      options.setFormatValidation(formatValidation);
    }
    return Validator.create(JsonSchema.of(DATE_SCHEMA.copy()), options);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testFormatIsAnnotationOnlyByDefaultSince201909() {
    // https://json-schema.org/draft/2020-12: format is an annotation by default
    assertThat(validator(Draft.DRAFT201909, null).validate("not-a-date").getValid()).isEqualTo(true);
    assertThat(validator(Draft.DRAFT202012, null).validate("not-a-date").getValid()).isEqualTo(true);
    // valid values are unaffected
    assertThat(validator(Draft.DRAFT202012, null).validate("2026-08-11").getValid()).isEqualTo(true);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testFormatIsAssertedByDefaultUpToDraft7() {
    assertThat(validator(Draft.DRAFT4, null).validate("not-a-date").getValid()).isEqualTo(false);
    assertThat(validator(Draft.DRAFT7, null).validate("not-a-date").getValid()).isEqualTo(false);
    assertThat(validator(Draft.DRAFT7, null).validate("2026-08-11").getValid()).isEqualTo(true);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testFormatValidationCanBeForcedOn() {
    assertThat(validator(Draft.DRAFT202012, true).validate("not-a-date").getValid()).isEqualTo(false);
    assertThat(validator(Draft.DRAFT202012, true).validate("2026-08-11").getValid()).isEqualTo(true);
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  public void testFormatValidationCanBeForcedOff() {
    assertThat(validator(Draft.DRAFT7, false).validate("not-a-date").getValid()).isEqualTo(true);
    assertThat(validator(Draft.DRAFT4, false).validate("not-a-date").getValid()).isEqualTo(true);
  }
}
