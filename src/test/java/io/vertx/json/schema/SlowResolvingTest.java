package io.vertx.json.schema;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class SlowResolvingTest {
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

    String resolvedApiPath = resourceFolder + "/api_resolved.json";
    JsonObject apiResolvedJson = new JsonObject(vertx.fileSystem().readFileBlocking(resolvedApiPath));

    Instant start = Instant.now();
    JsonObject resolved = repository.resolve(JsonSchema.of(apiJson));
    long requiredTime = ChronoUnit.SECONDS.between(start, Instant.now());

    // It is bad to use number of seconds here, because on slower machines it maybe takes longer. But for this test
    // it doesn't matter if its 5 or 10 seconds, it should just ensure that resolving doesn't take several minutes, as before.
    assertThat(requiredTime).isLessThan(10L);
    assertThat(resolved).isEqualTo(apiResolvedJson);
  }
}
