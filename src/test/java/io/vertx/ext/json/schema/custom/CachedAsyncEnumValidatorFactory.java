package io.vertx.ext.json.schema.custom;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class CachedAsyncEnumValidatorFactory implements ValidatorFactory {

  Vertx vertx;

  public CachedAsyncEnumValidatorFactory(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String address = (String) schema.getValue("asyncEnum");
      return new CachedAsyncEnumValidator(vertx, address, parent);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for propertiesMultipleOf keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null propertiesMultipleOf keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("asyncEnum");
  }

  private class CachedAsyncEnumValidator extends BaseMutableStateValidator {
    Vertx vertx;
    String address;
    AtomicReference<Optional<JsonArray>> cache;

    public CachedAsyncEnumValidator(Vertx vertx, String address, MutableStateValidator parent) {
      super(parent);
      this.vertx = vertx;
      this.address = address;
      this.cache = new AtomicReference<>();

      vertx.eventBus().consumer(address + "_invalidate_cache", m -> {
        cache.set(Optional.empty());
        this.triggerUpdateIsSync();
      });
    }

    @Override
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (!this.cache.get().get().contains(in)) throw createException("Not matching cached async enum", "asyncEnum", in);
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      Promise<Void> promise = Promise.promise();
      vertx.eventBus().request(address, new JsonObject(), ar -> {
        JsonArray enumValues = (JsonArray) ar.result().body();

        // Write cache
        this.cache.set(Optional.of(enumValues));
        this.triggerUpdateIsSync();

        if (!enumValues.contains(in)) promise.fail(createException("Not matching async enum", "asyncEnum", in));
        else promise.complete();
      });
      return promise.future();
    }

    @Override
    public boolean calculateIsSync() {
      return cache.get().isPresent();
    }

  }

}
