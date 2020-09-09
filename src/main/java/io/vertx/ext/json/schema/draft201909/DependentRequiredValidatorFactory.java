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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class DependentRequiredValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject dependencies = schema.getJsonObject("dependentRequired");
      return new DependentRequiredValidator(
        dependencies
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, entry -> ((JsonArray) entry.getValue()).stream().map(v -> (String) v).collect(Collectors.toSet())))
      );
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for dependentRequired keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null dependentRequired keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("dependentRequired");
  }

  class DependentRequiredValidator extends BaseSyncValidator {

    public final Map<String, Set<String>> keyDeps;

    public DependentRequiredValidator(Map<String, Set<String>> keyDeps) {
      this.keyDeps = keyDeps;
    }

    private void checkKeyDeps(JsonObject obj) {
      Set<String> objKeys = obj.getMap().keySet();
      for (Map.Entry<String, Set<String>> dependency : keyDeps.entrySet()) {
        if (obj.containsKey(dependency.getKey()) && !objKeys.containsAll(dependency.getValue()))
          throw createException("dependencies of key " + dependency.getKey() + " are not satisfied: " + dependency.getValue().toString(), "dependentRequired", obj);
      }
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        checkKeyDeps(obj);
      }
    }
  }

}
