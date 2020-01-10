package io.vertx.ext.json.schema.custom;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.draft7.Draft7SchemaParser;
import io.vertx.ext.json.schema.common.SchemaParserInternal;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(VertxExtension.class)
public class CustomValidatorTests {

  private Consumer<ValidationException> errorKeyword(String keyword) {
    return (ve) -> assertThat(ve.keyword()).isEqualTo(keyword);
  }

  @Test
  public void propertiesMultipleOf(Vertx vertx) throws IOException {
    URI u = buildBaseUri("custom", "properties_multiple_of.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser
        .create(router)
        .withValidatorFactory(new PropertiesMultipleOfValidatorFactory());
    Schema schema = parser.parse(obj, u);

    assertThat(schema)
        .isSync();
    assertThatCode(() -> schema.validateSync(
        new JsonObject().putNull("a").putNull("b").putNull("c")
    )).doesNotThrowAnyException();
    assertThatExceptionOfType(ValidationException.class)
        .isThrownBy(() -> schema.validateSync(new JsonObject().putNull("a").putNull("b").putNull("c").putNull("d")))
        .withMessage("The provided object size is not a multiple of 3")
        .satisfies(errorKeyword("propertiesMultipleOf"));
  }

  @Test
  public void cachedAsyncEnum(Vertx vertx, VertxTestContext testContext) throws IOException {
    // Set eb message consumers
    vertx.eventBus().consumer("names_address", m -> m.reply(new JsonArray().add("francesco").add("mario").add("luigi")));
    vertx.eventBus().consumer("devices_address", m -> m.reply(new JsonArray().add("smartphone").add("tv").add("laptop")));

    JsonObject validJson = new JsonObject()
        .put("names", new JsonArray().add("francesco").add("mario"))
        .put("devices", new JsonArray().add("smartphone").add("tv"));

    JsonObject invalidJson = new JsonObject()
        .put("names", new JsonArray().add("francesco").add("mario"))
        .put("devices", new JsonArray().add("francesco").add("tv"));

    URI u = buildBaseUri("custom", "async_with_ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser
        .create(router)
        .withValidatorFactory(new CachedAsyncEnumValidatorFactory(vertx));
    Schema schema = parser.parse(obj, u);

    Checkpoint cp = testContext.checkpoint(2);

    assertThat(schema)
        .isAsync();

    // 1. Validate async
    // 2. Check if sync state is propagated
    // 3. Validate sync
    // 4. Invalidate cache
    // 5. Re-validate async
    // 6. Re-check if sync state is propagated

    schema
        .validateAsync(validJson)
        .setHandler(testContext.succeeding(v -> {
          testContext.verify(() -> {
            assertThat(schema).isSync();
            assertThatCode(() -> schema.validateSync(validJson)).doesNotThrowAnyException();
            assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> schema.validateSync(invalidJson))
                .withMessage("Not matching cached async enum")
                .satisfies(errorKeyword("asyncEnum"));
          });
          cp.flag();

          // Invalid cache of the validator
          vertx.eventBus().send("names_address_invalidate_cache", new JsonObject());

          vertx.setTimer(100, l -> {
            assertThat(schema).isAsync();
            testContext.assertComplete(schema.validateAsync(validJson)).setHandler(ar -> {
              testContext.verify(() -> {
                assertThat(schema).isSync();
              });
              cp.flag();
            });
          });
        }));
  }

    @Test
    public void asyncEnum(Vertx vertx, VertxTestContext testContext) throws IOException {
      // Set eb message consumers
      vertx.eventBus().consumer("names_address", m -> m.reply(new JsonArray().add("francesco").add("mario").add("luigi")));
      vertx.eventBus().consumer("devices_address", m -> m.reply(new JsonArray().add("smartphone").add("tv").add("laptop")));

      URI u = buildBaseUri("custom", "async_with_ref.json");
      JsonObject obj = loadJson(u);
      SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
      SchemaParserInternal parser = Draft7SchemaParser
          .create(router)
          .withValidatorFactory(new AsyncEnumValidatorFactory(vertx));
      Schema schema = parser.parse(obj, u);

      Checkpoint cp = testContext.checkpoint(2);

      assertThat(schema)
          .isAsync();

      schema.validateAsync(new JsonObject()
          .put("names", new JsonArray().add("francesco").add("mario"))
          .put("devices", new JsonArray().add("smartphone").add("tv"))
      ).setHandler(testContext.succeeding(v -> cp.flag()));

      schema.validateAsync(new JsonObject()
          .put("names", new JsonArray().add("francesco").add("mario"))
          .put("devices", new JsonArray().add("francesco").add("tv"))
      ).setHandler(testContext.failing(c -> {
        testContext.verify(() ->
            assertThat(c)
                .isInstanceOfSatisfying(ValidationException.class, errorKeyword("asyncEnum"))
                .hasMessage("Not matching async enum")
        );
        cp.flag();
      }));
    }

}
