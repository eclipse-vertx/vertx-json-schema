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
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

public class AsyncEnumValidatorFactory implements ValidatorFactory {

  Vertx vertx;

  public AsyncEnumValidatorFactory(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String address = (String) schema.getValue("asyncEnum");
      return new AsyncEnumValidator(vertx, address);
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

  private class AsyncEnumValidator extends BaseAsyncValidator {

    Vertx vertx;
    String address;

    public AsyncEnumValidator(Vertx vertx, String address) {
      this.vertx = vertx;
      this.address = address;
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      Promise<Void> fut = Promise.promise();
      vertx.eventBus().request(address, new JsonObject()).onComplete(ar -> {
        JsonArray enumValues = (JsonArray) ar.result().body();
        if (!enumValues.contains(in))
          fut.fail(ValidationException.create("Not matching async enum", "asyncEnum", in));
        else fut.complete();
      });
      return fut.future();
    }
  }

}
