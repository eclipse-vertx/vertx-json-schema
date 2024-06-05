package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.vertx.json.schema.Draft.DRAFT201909;
import static io.vertx.json.schema.Draft.DRAFT202012;
import static io.vertx.json.schema.Draft.DRAFT4;
import static io.vertx.json.schema.Draft.DRAFT7;

@ExtendWith(VertxExtension.class)
class SchemaDefinitionValidationTest {

  private static final Path RESOURCE_PATH = Paths.get("src", "test", "resources", "schema_definition_validation");

  private static Stream<Arguments> testSchemaDefinitionValidation() {
    return Stream.of(
      Arguments.of(DRAFT4, RESOURCE_PATH.resolve("OpenAPI3_0.json")),
      Arguments.of(DRAFT7, RESOURCE_PATH.resolve("angular_cli_workspace_schema.json")),
      Arguments.of(DRAFT201909, RESOURCE_PATH.resolve("compose_spec.json")),
      Arguments.of(DRAFT202012, RESOURCE_PATH.resolve("OpenAPI3_1.json"))
    );
  }

  @ParameterizedTest(name = "{index} test preloadMetaSchema with draft {0}")
  @MethodSource
  void testSchemaDefinitionValidation(Draft draft, Path schemaPath, Vertx vertx) throws IOException {
    JsonSchemaOptions opts = new JsonSchemaOptions().setBaseUri("https://example.org");
    SchemaRepository repo = SchemaRepository.create(opts);
    repo.preloadMetaSchema(vertx.fileSystem(), draft);

    Buffer schemaBuffer = Buffer.buffer(Files.readAllBytes(schemaPath));
    JsonSchema schemaToValidate = JsonSchema.of(schemaBuffer.toJsonObject());

    OutputUnit res = repo.validator(draft.getIdentifier()).validate(schemaToValidate);
    Assertions.assertTrue(res.getValid());
    Assertions.assertEquals(OutputErrorType.NONE, res.getErrorType());
  }
}
