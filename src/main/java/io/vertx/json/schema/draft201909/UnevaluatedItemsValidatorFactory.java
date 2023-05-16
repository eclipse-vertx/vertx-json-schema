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

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vertx.json.schema.common.JsonUtil.unwrap;

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

  static class SchemedUnevaluatedItemsValidator extends BaseSingleSchemaValidator {

    public SchemedUnevaluatedItemsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, final Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      Object o = unwrap(in);
      if (o instanceof List<?>) {
        List<?> arr = (List<?>) o;
        Set<Integer> unevaluatedItems = computeUnevaluatedItems(context, arr);

        return Future.all(
          unevaluatedItems
            .stream()
            .map(index -> schema.validateAsync(context.lowerLevelContext(index), arr.get(index)))
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
      in = unwrap(in);
      if (in instanceof List<?>) {
        List<?> arr = (List<?>) in;
        Set<Integer> unevaluatedProperties = computeUnevaluatedItems(context, arr);

        unevaluatedProperties.forEach(index ->
          schema.validateSync(context.lowerLevelContext(index), arr.get(index))
        );
      }
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.CONTEXTUAL_VALIDATOR;
    }

    private Set<Integer> computeUnevaluatedItems(ValidatorContext context, List<?> in) {
      return SetUtils.minus(
        SetUtils.range(0, in.size()),
        context.evaluatedItems()
      );
    }
  }

  static class NoUnevaluatedItemsValidator extends BaseSyncValidator {

    @Override
    public void validateSync(ValidatorContext context, final Object in) throws ValidationException, NoSyncValidationException {
      Object o = unwrap(in);
      if (o instanceof List<?>) {
        List<?> arr = (List<?>) o;
        if (arr.size() != context.evaluatedItems().size()) {
          throw ValidationException.create(
            "Expecting no unevaluated items. Unevaluated items: " +
              SetUtils.minus(
                SetUtils.range(0, arr.size()),
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
