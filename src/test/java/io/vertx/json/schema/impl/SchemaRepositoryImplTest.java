package io.vertx.json.schema.impl;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.junit5.VertxExtension;
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
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_201909_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_202012_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_4_META_FILES;
import static io.vertx.json.schema.impl.SchemaRepositoryImpl.DRAFT_7_META_FILES;
import static org.mockito.ArgumentMatchers.endsWith;
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
    JsonSchemaOptions opts = new JsonSchemaOptions().setDraft(draft).setBaseUri("https://example.org");
    SchemaRepository repo = SchemaRepository.create(opts);
    FileSystem fileSystemSpy = spy(vertx.fileSystem());
    repo.preloadMetaSchema(fileSystemSpy);

    for(String id : ids) {
      String classpath = id.replace("http://", "").replace("https://", "");
      verify(fileSystemSpy).readFileBlocking(endsWith(classpath));
    }
  }
}
