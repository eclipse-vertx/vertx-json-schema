package io.vertx.tests.impl;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonFormatValidator;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.json.schema.impl.SchemaRepositoryImpl;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.vertx.json.schema.Draft.DRAFT201909;
import static io.vertx.json.schema.Draft.DRAFT202012;
import static io.vertx.json.schema.Draft.DRAFT4;
import static io.vertx.json.schema.Draft.DRAFT7;
import static io.vertx.json.schema.OutputFormat.Basic;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_201909_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_202012_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_4_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_7_META_FILES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(VertxExtension.class)
class SchemaRepositoryImplTest {

  private static Stream<Arguments> testPreloadMetaSchema() {
    return Stream.of(
      Arguments.of(DRAFT4, DRAFT_4_META_FILES),
      Arguments.of(DRAFT7, DRAFT_7_META_FILES),
      Arguments.of(DRAFT201909, DRAFT_201909_META_FILES),
      Arguments.of(DRAFT202012, DRAFT_202012_META_FILES)
    );
  }

  @ParameterizedTest(name = "{index} test preloadMetaSchema with draft {0}")
  @MethodSource
  void testPreloadMetaSchema(Draft draft, List<String> ids, Vertx vertx) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setBaseUri("https://example.org");
    SchemaRepository repo = SchemaRepository.create(opts);
    FileSystem fileSystemSpy = spy(vertx.fileSystem());
    repo.preloadMetaSchema(fileSystemSpy, draft);

    for(String id : ids) {
      String classpath = id.replace("http://", "").replace("https://", "");
      verify(fileSystemSpy).readFileBlocking(endsWith(classpath));
    }
  }

  @Test
  @DisplayName("preloadMetaSchema(fs) should throw an error if no draft is set in the options")
  void testPreloadMetaSchemaException(Vertx vertx) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setBaseUri("https://example.org");
    SchemaRepository repo = SchemaRepository.create(opts);

    assertThrows(IllegalStateException.class, () -> repo.preloadMetaSchema(vertx.fileSystem()));
  }

  @Test
  @DisplayName("preloadMetaSchema(fs) should get the draft from the options")
  void testPreloadMetaSchemaDraftFromOptions(Vertx vertx) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setBaseUri("https://example.org").setDraft(DRAFT4);
    SchemaRepository repoSpy = spy(SchemaRepository.create(opts));
    repoSpy.preloadMetaSchema(vertx.fileSystem());

    verify(repoSpy).preloadMetaSchema(any(), eq(DRAFT4));
  }

  @Test
  public void testThrowErrorNoFormatValidator() {
    JsonSchemaOptions dummyOptions = new JsonSchemaOptions().setBaseUri("app://").setDraft(DRAFT201909);
    NullPointerException exception = assertThrows(NullPointerException.class,
      () -> new SchemaRepositoryImpl(dummyOptions, null));
    assertThat(exception).hasMessage("'formatValidator' cannot be null");
  }

  @Test
  public void testFormatValidatorIsPassed() {
    JsonSchemaOptions options =
      new JsonSchemaOptions().setBaseUri("https://vertx.io").setDraft(Draft.DRAFT202012).setOutputFormat(Basic);
    JsonSchema dummySchema = JsonSchema.of(Schemas.stringSchema().withKeyword("format", "noFoobar").toJson());

    OutputUnit ouSuccess = SchemaRepository.create(options).validator(dummySchema).validate("foobar");
    assertThat(ouSuccess.getValid()).isTrue();

    JsonFormatValidator formatValidator = (instanceType, format, instance) -> {
      if (instanceType == "string" && "noFoobar".equals(format) && "foobar".equalsIgnoreCase(instance.toString())) {
        return "no foobar allowed";
      }
      return null;
    };
    OutputUnit ouFailed = SchemaRepository.create(options, formatValidator).validator(dummySchema).validate("foobar");
    assertThat(ouFailed.getValid()).isFalse();
    assertThat(ouFailed.getErrors()).hasSize(1);
    assertThat(ouFailed.getErrors().get(0).getError()).isEqualTo("no foobar allowed");
  }
}
