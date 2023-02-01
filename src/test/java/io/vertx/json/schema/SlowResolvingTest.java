package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ExtendWith(VertxExtension.class)
class SlowResolvingTest {
  String apiSmallPath = "Plattform_i40-Entire-API-Collection-V3.0-swagger-small.yaml";

  String apiResolvedPath = "Plattform_i40-Entire-API-Collection-V3.0-resolved.yaml";

  String aas = "digital-twin-server-openapi.yaml";

  @Test
  public void testSlowResolving(Vertx vertx) {
    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions().setDraft(Draft.DRAFT4).setBaseUri("app://"));
    repository.preloadMetaSchema(vertx.fileSystem());

    String resourceFolder = "slow_resolving";

    String metaModellRef = "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0";
    String metaModellPath = resourceFolder + "/meta-model/V3.0.json";
    JsonObject metaModelJson = new JsonObject(vertx.fileSystem().readFileBlocking(metaModellPath));
    repository.dereference(metaModellRef, JsonSchema.of(metaModelJson));

    String apiSchemaRef = "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0";
    String apiSchemaPath = resourceFolder + "/api-schemas/V3.0.json";
    JsonObject apiSchemasJson = new JsonObject(vertx.fileSystem().readFileBlocking(apiSchemaPath));
    repository.dereference(apiSchemaRef, JsonSchema.of(apiSchemasJson));

    String apiJsonPath = resourceFolder + "/api.json";
    JsonObject apiJson = new JsonObject(vertx.fileSystem().readFileBlocking(apiJsonPath));
    repository.dereference(JsonSchema.of(apiJson));

    Instant start = Instant.now();
    JsonObject resolved = repository.resolve(JsonSchema.of(apiJson));
    System.out.println(ChronoUnit.SECONDS.between(start, Instant.now()));
  }
}
