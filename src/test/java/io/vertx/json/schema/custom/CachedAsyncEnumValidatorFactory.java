/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.custom;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (!this.cache.get().get().contains(in))
        throw ValidationException.create("Not matching cached async enum", "asyncEnum", in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      Promise<Void> promise = Promise.promise();
      vertx.eventBus().request(address, new JsonObject(), ar -> {
        JsonArray enumValues = (JsonArray) ar.result().body();

        // Write cache
        this.cache.set(Optional.of(enumValues));
        this.triggerUpdateIsSync();

        if (!enumValues.contains(in))
          promise.fail(ValidationException.create("Not matching async enum", "asyncEnum", in));
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
