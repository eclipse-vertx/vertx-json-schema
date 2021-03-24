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
package io.vertx.json.schema.draft7;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.vertx.json.schema.common.JsonUtil.isArray;
import static io.vertx.json.schema.common.JsonUtil.unwrap;

public class ItemsValidatorFactory extends io.vertx.json.schema.common.ItemsValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    Object itemsSchema = schema.getValue("items");
    if (itemsSchema instanceof JsonArray) {
      try {
        JsonPointer baseScope = scope.copy().append("items");
        JsonArray itemsList = (JsonArray) itemsSchema;
        List<SchemaInternal> parsedSchemas = new ArrayList<>();

        ItemByItemValidator validator = new ItemByItemValidator(parent);
        for (int i = 0; i < itemsList.size(); i++) {
          parsedSchemas.add(i, parser.parse(itemsList.getValue(i), baseScope.copy().append(Integer.toString(i)), validator));
        }
        if (schema.containsKey("additionalItems"))
          validator.configure(parsedSchemas.toArray(new SchemaInternal[parsedSchemas.size()]), parser.parse(schema.getValue("additionalItems"), scope.copy().append("additionalItems"), validator));
        else
          validator.configure(parsedSchemas.toArray(new SchemaInternal[parsedSchemas.size()]), null);
        return validator;
      } catch (NullPointerException e) {
        throw new SchemaException(schema, "Null items keyword", e);
      }
    } else {
      return super.createValidator(schema, scope, parser, parent);
    }
  }

  class ItemByItemValidator extends BaseMutableStateValidator implements DefaultApplier {

    SchemaInternal[] schemas;
    SchemaInternal additionalItems;

    public ItemByItemValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(SchemaInternal[] schemas, SchemaInternal additionalItems) {
      this.schemas = schemas;
      this.additionalItems = additionalItems;
      initializeIsSync();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      in = unwrap(in);
      if (in instanceof List<?>) {
        List<?> arr = (List<?>) in;
        for (int i = 0; i < arr.size(); i++) {
          if (i >= schemas.length) {
            if (additionalItems != null) {
              context.markEvaluatedItem(i);
              additionalItems.validateSync(context.lowerLevelContext(i), arr.get(i));
            }
          } else {
            context.markEvaluatedItem(i);
            schemas[i].validateSync(context.lowerLevelContext(i), arr.get(i));
          }
        }
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      in = unwrap(in);
      if (in instanceof List<?>) {
        List<?> arr = (List<?>) in;
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
          Future<Void> fut;
          if (i >= schemas.length) {
            if (additionalItems != null) {
              context.markEvaluatedItem(i);
              fut = additionalItems.validateAsync(context.lowerLevelContext(i), arr.get(i));
            } else continue;
          } else {
            context.markEvaluatedItem(i);
            fut = schemas[i].validateAsync(context.lowerLevelContext(i), arr.get(i));
          }
          if (fut.isComplete()) {
            if (fut.failed()) return Future.failedFuture(fut.cause());
          } else {
            futures.add(fut);
          }
        }
        if (futures.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futures).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public boolean calculateIsSync() {
      return (additionalItems == null || additionalItems.isSync()) && Arrays.stream(schemas).map(Schema::isSync).reduce(true, Boolean::logicalAnd);
    }

    @Override
    public Future<Void> applyDefaultValue(Object value) {
      if (!isArray(value)) {
        return Future.succeededFuture();
      }

      List<Future> futures = new ArrayList<>();
      value = unwrap(value);
      List<?> arr = (List<?>) value;
      for (int i = 0; i < arr.size(); i++) {
        Object valToDefault = arr.get(i);
        if (i >= schemas.length) {
          if (additionalItems != null) {
            if (additionalItems.isSync()) {
              additionalItems.getOrApplyDefaultSync(valToDefault);
            } else {
              futures.add(
                additionalItems.getOrApplyDefaultAsync(valToDefault)
              );
            }
          }
        } else {
          SchemaInternal schema = schemas[i];
          if (schema.isSync()) {
            schemas[i].getOrApplyDefaultSync(valToDefault);
          } else {
            futures.add(
              schemas[i].getOrApplyDefaultAsync(valToDefault)
            );
          }
        }
      }

      if (futures.isEmpty()) {
        return Future.succeededFuture();
      }

      return CompositeFuture.all(futures).mapEmpty();
    }
  }

}
