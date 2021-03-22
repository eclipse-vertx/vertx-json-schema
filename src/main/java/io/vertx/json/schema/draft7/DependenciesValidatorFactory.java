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
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependenciesValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject dependencies = schema.getJsonObject("dependencies");
      JsonPointer baseScope = scope.copy().append("dependencies");
      Map<String, Set<String>> keyDeps = new HashMap<>();
      Map<String, SchemaInternal> keySchemaDeps = new HashMap<>();

      DependenciesValidator validator = new DependenciesValidator(parent);

      for (Map.Entry<String, Object> entry : dependencies.getMap().entrySet()) {
        if (entry.getValue() instanceof Map || entry.getValue() instanceof Boolean) {
          keySchemaDeps.put(entry.getKey(), parser.parse((entry.getValue() instanceof Map) ? new JsonObject((Map<String, Object>) entry.getValue()) : entry.getValue(), baseScope.copy().append(entry.getKey()), validator));
        } else {
          if (!((List) entry.getValue()).isEmpty())
            keyDeps.put(entry.getKey(), ((List<String>) entry.getValue()).stream().collect(Collectors.toSet()));
        }
      }
      validator.configure(keyDeps, keySchemaDeps);
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for dependencies keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null dependencies keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("dependencies");
  }

  class DependenciesValidator extends BaseMutableStateValidator {

    Map<String, Set<String>> keyDeps;
    Map<String, SchemaInternal> keySchemaDeps;

    public DependenciesValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(Map<String, Set<String>> keyDeps, Map<String, SchemaInternal> keySchemaDeps) {
      this.keyDeps = keyDeps;
      this.keySchemaDeps = keySchemaDeps;
      initializeIsSync();
    }

    private void checkKeyDeps(JsonObject obj) {
      Set<String> objKeys = obj.getMap().keySet();
      for (Map.Entry<String, Set<String>> dependency : keyDeps.entrySet()) {
        if (obj.containsKey(dependency.getKey()) && !objKeys.containsAll(dependency.getValue()))
          throw ValidationException.create("dependencies of key " + dependency.getKey() + " are not satisfied: " + dependency.getValue().toString(), "dependencies", obj);
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        try {
          checkKeyDeps(obj);
        } catch (ValidationException e) {
          return Future.failedFuture(e);
        }
        List<Future> futs = keySchemaDeps
          .entrySet()
          .stream()
          .filter(e -> obj.containsKey(e.getKey()))
          .map(e -> e.getValue().validateAsync(context, in))
          .collect(Collectors.toList());
        if (futs.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futs).mapEmpty();
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        checkKeyDeps(obj);
        keySchemaDeps
          .entrySet()
          .stream()
          .filter(e -> obj.containsKey(e.getKey()))
          .forEach(e -> e.getValue().validateSync(context, in));
      }
    }

    @Override
    public boolean calculateIsSync() {
      return keySchemaDeps.values().stream().map(Schema::isSync).reduce(true, Boolean::logicalAnd);
    }
  }

}
