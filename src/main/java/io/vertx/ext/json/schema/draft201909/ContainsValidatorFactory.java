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
package io.vertx.ext.json.schema.draft201909;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class ContainsValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Object containsSchema = schema.getValue("contains");
      BoundedContainsValidator validator = new BoundedContainsValidator(parent, schema.getInteger("minContains"), schema.getInteger("maxContains"));
      validator.setSchema(parser.parse(containsSchema, scope.append("contains"), validator));
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for maximum or exclusiveMaximum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null maximum or exclusiveMaximum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("contains");
  }

  class BoundedContainsValidator extends BaseSingleSchemaValidator {

    private final int min;
    private final Integer max;

    public BoundedContainsValidator(MutableStateValidator parent, Integer min, Integer max) {
      super(parent);
      this.min = min == null ? 1 : min;
      this.max = max;
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (min == 0) {
        return Future.succeededFuture();
      }
      if (in instanceof JsonArray) {
        if (((JsonArray) in).isEmpty())
          return Future.failedFuture(createException("provided array should not be empty", "contains", in));
        else
          return CompositeFuture.any(
            ((JsonArray) in).stream().map(v -> schema.validateAsync(context.lowerLevelContext(), v)).collect(Collectors.toList())
          ).compose(
            cf -> {
              IntStream.rangeClosed(0, cf.size())
                .forEach(i -> {
                  if (cf.succeeded(i)) {
                    context.markEvaluatedItem(i);
                  }
                });
              int matches = cf.size();
              if (matches < min) {
                return Future.failedFuture(createException("provided array doesn't contain " + min + " elements matching the contains schema", "contains", in));
              }
              if (max != null && matches > max) {
                return Future.failedFuture(createException("provided array contains more than " + max + " elements matching the contains schema", "contains", in));
              }
              return Future.succeededFuture();
            },
            err -> Future.failedFuture(createException("provided array doesn't contain any element matching the contains schema", "contains", in, err))
          );
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      if (min == 0) {
        return;
      }
      this.checkSync();
      ValidationException t = null;
      int matches = 0;
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          try {
            schema.validateSync(context.lowerLevelContext(), arr.getValue(i));
            context.markEvaluatedItem(i);
            matches++;
          } catch (ValidationException e) {
            t = e;
          }
        }
      }
      if (matches < min) {
        throw createException("provided array doesn't contain " + min + " elements matching the contains schema", "contains", in, t);
      }
      if (max != null && matches > max) {
        throw createException("provided array contains more than " + max + " elements matching the contains schema", "contains", in, t);
      }
    }
  }

}
