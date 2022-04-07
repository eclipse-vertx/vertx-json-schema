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
package io.vertx.json.schema.draft202012;

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

public class PrefixItemsValidatorFactory extends io.vertx.json.schema.draft7.ItemsValidatorFactory {

  @Override
  protected String getKeyword() {
    return "prefixItems";
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    Object prefixItemsSchema = schema.getValue("prefixItems");
    if (prefixItemsSchema instanceof JsonArray) {
      try {
        JsonPointer baseScope = scope.copy().append("prefixItems");
        JsonArray itemsList = (JsonArray) prefixItemsSchema;
        List<SchemaInternal> parsedSchemas = new ArrayList<>();

        ItemByItemValidator validator = new ItemByItemValidator(parent);
        for (int i = 0; i < itemsList.size(); i++) {
          parsedSchemas.add(i, parser.parse(itemsList.getValue(i), baseScope.copy().append(Integer.toString(i)), validator));
        }
        if (schema.containsKey("items"))
          validator.configure(parsedSchemas.toArray(new SchemaInternal[0]), parser.parse(schema.getValue("items"), scope.copy().append("items"), validator));
        else
          validator.configure(parsedSchemas.toArray(new SchemaInternal[0]), null);
        return validator;
      } catch (NullPointerException e) {
        throw new SchemaException(schema, "Null items keyword", e);
      }
    } else {
      return super.createValidator(schema, scope, parser, parent);
    }
  }

  static class ItemByItemValidator extends BaseMutableStateValidator implements DefaultApplier {

    SchemaInternal[] schemas;
    SchemaInternal items;

    public ItemByItemValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(SchemaInternal[] schemas, SchemaInternal items) {
      this.schemas = schemas;
      this.items = items;
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
            if (items != null) {
              context.markEvaluatedItem(i);
              items.validateSync(context.lowerLevelContext(i), arr.get(i));
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
            if (items != null) {
              context.markEvaluatedItem(i);
              fut = items.validateAsync(context.lowerLevelContext(i), arr.get(i));
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
      return (items == null || items.isSync()) && Arrays.stream(schemas).map(Schema::isSync).reduce(true, Boolean::logicalAnd);
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
          if (items != null) {
            if (items.isSync()) {
              items.getOrApplyDefaultSync(valToDefault);
            } else {
              futures.add(
                items.getOrApplyDefaultAsync(valToDefault)
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
