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
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.Set;
import java.util.stream.Collectors;

public class UnevaluatedPropertiesValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) throws SchemaException {
    try {
      Object unevaluatedProperties = schema.getValue("unevaluatedProperties");
      if (unevaluatedProperties instanceof Boolean) {
        if (!((Boolean) unevaluatedProperties)) {
          return new NoUnevaluatedPropertiesValidator();
        } else {
          // unevaluatedProperties == true doesn't need any validation
          return null;
        }
      }
      BaseSingleSchemaValidator validator = new SchemedUnevaluatedPropertiesValidator(parent);
      validator.setSchema(parser.parse(unevaluatedProperties, scope.append("unevaluatedProperties"), validator));
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for unevaluatedProperties keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null unevaluatedProperties keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("unevaluatedProperties");
  }

  class SchemedUnevaluatedPropertiesValidator extends BaseSingleSchemaValidator {

    public SchemedUnevaluatedPropertiesValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        Set<String> unevaluatedItems = computeUnevaluatedProperties(context, obj);

        return CompositeFuture.all(
          unevaluatedItems
            .stream()
            .map(key -> schema.validateAsync(context.lowerLevelContext(), obj.getValue(key)))
            .collect(Collectors.toList())
        )
          .recover(t -> Future.failedFuture(ValidationException.createException(
            "one of the unevaluated properties doesn't match the unevaluatedProperties schema",
            "unevaluatedProperties",
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
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        Set<String> unevaluatedProperties = computeUnevaluatedProperties(context, obj);

        unevaluatedProperties.forEach(key ->
          schema.validateSync(context.lowerLevelContext(), obj.getValue(key))
        );
      }
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.CONTEXTUAL_VALIDATOR;
    }

    private Set<String> computeUnevaluatedProperties(ValidatorContext context, JsonObject in) {
      return SetUtils.minus(
        in.fieldNames(),
        context.evaluatedProperties()
      );
    }
  }

  class NoUnevaluatedPropertiesValidator extends BaseSyncValidator {

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      if (in instanceof JsonObject) {
        if (!context.evaluatedProperties().containsAll(((JsonObject) in).fieldNames())) {
          throw ValidationException.createException(
            "Expecting no unevaluated properties. Unevaluated properties: " +
              SetUtils.minus(
                ((JsonObject) in).fieldNames(),
                context.evaluatedProperties()
              ),
            "unevaluatedProperties",
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
