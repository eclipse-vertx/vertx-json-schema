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
package io.vertx.json.schema.openapi3;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.common.*;

public class MinimumValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("minimum");
      if (schema.containsKey("exclusiveMinimum") && schema.getBoolean("exclusiveMinimum"))
        return new ExclusiveMinimumValidator(maximum.doubleValue());
      return new MinimumValidator(maximum.doubleValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minimum or exclusiveMinimum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minimum or exclusiveMinimum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minimum");
  }

}
