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
package io.vertx.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;

import java.math.BigDecimal;

public class MultipleOfValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number multipleOf = (Number) schema.getValue("multipleOf");
      return new MultipleOfValidator(multipleOf);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for multipleOf keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null multipleOf keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("multipleOf");
  }

  static class MultipleOfValidator extends BaseSyncValidator {
    private final Long multipleOfL;
    private final BigDecimal multipleOfD;

    public MultipleOfValidator(Number multipleOf) {
      // can be null to signal integer arithmetic
      multipleOfD = toBigDecimal(multipleOf, false);
      if (multipleOfD == null) {
        multipleOfL = multipleOf.longValue();
      } else {
        multipleOfL = null;
      }
    }

    private BigDecimal toBigDecimal(Number in, boolean force) {
      if (in instanceof BigDecimal) {
        return (BigDecimal) in;
      }
      if (in instanceof Float) {
        return BigDecimal.valueOf(in.floatValue());
      }
      if (in instanceof Double) {
        return BigDecimal.valueOf(in.doubleValue());
      }
      if (force) {
        return BigDecimal.valueOf(in.longValue());
      }
      return null;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof Number) {
        // floating point arithmetic,
        // if the multipleOf is decimal, we always need to handle this operation as decimal
        final BigDecimal inBD = toBigDecimal((Number) in, multipleOfD != null);
        if (inBD != null) {
          if (inBD.remainder(multipleOfD).compareTo(BigDecimal.ZERO) != 0) {
            throw ValidationException.create("provided number should be multiple of " + multipleOfD, "multipleOf", in);
          }
        } else {
          // integer arithmetic, fallback for simpler arithmetic
          if (((Number) in).longValue() % multipleOfL != 0L) {
            throw ValidationException.create("provided number should be multiple of " + multipleOfL, "multipleOf", in);
          }
        }
      }
    }
  }

}
