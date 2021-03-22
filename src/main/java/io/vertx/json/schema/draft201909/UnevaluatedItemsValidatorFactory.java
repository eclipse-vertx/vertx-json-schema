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
package io.vertx.json.schema.draft201909;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.Set;
import java.util.stream.Collectors;

public class UnevaluatedItemsValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) throws SchemaException {
    try {
      Object unevaluatedItems = schema.getValue("unevaluatedItems");
      if (unevaluatedItems instanceof Boolean) {
        if (!((Boolean) unevaluatedItems)) {
          return new NoUnevaluatedItemsValidator();
        } else {
          // unevaluatedItems == true doesn't need any validation
          return null;
        }
      }
      BaseSingleSchemaValidator validator = new SchemedUnevaluatedItemsValidator(parent);
      validator.setSchema(parser.parse(unevaluatedItems, scope.append("unevaluatedItems"), validator));
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for unevaluatedItems keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null unevaluatedItems keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("unevaluatedItems");
  }

  class SchemedUnevaluatedItemsValidator extends BaseSingleSchemaValidator {

    public SchemedUnevaluatedItemsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonArray) {
        JsonArray array = (JsonArray) in;
        Set<Integer> unevaluatedItems = computeUnevaluatedItems(context, array);

        return CompositeFuture.all(
          unevaluatedItems
            .stream()
            .map(index -> schema.validateAsync(context.lowerLevelContext(index), array.getValue(index)))
            .collect(Collectors.toList())
        )
          .recover(t -> Future.failedFuture(ValidationException.create(
            "one of the unevaluated items doesn't match the unevaluatedItems schema",
            "unevaluatedItems",
            in,
            t
          )))
          .mapEmpty();
      }

      return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonArray) {
        JsonArray array = (JsonArray) in;
        Set<Integer> unevaluatedProperties = computeUnevaluatedItems(context, array);

        unevaluatedProperties.forEach(index ->
          schema.validateSync(context.lowerLevelContext(index), array.getValue(index))
        );
      }
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.CONTEXTUAL_VALIDATOR;
    }

    private Set<Integer> computeUnevaluatedItems(ValidatorContext context, JsonArray in) {
      return SetUtils.minus(
        SetUtils.range(0, in.size()),
        context.evaluatedItems()
      );
    }
  }

  class NoUnevaluatedItemsValidator extends BaseSyncValidator {

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      if (in instanceof JsonArray) {
        if (((JsonArray) in).size() != context.evaluatedItems().size()) {
          throw ValidationException.create(
            "Expecting no unevaluated items. Unevaluated items: " +
              SetUtils.minus(
                SetUtils.range(0, (((JsonArray) in).size())),
                context.evaluatedItems()
              ),
            "unevaluatedItems",
            in
          );
        }
      }
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.CONTEXTUAL_VALIDATOR;
    }
  }

}
