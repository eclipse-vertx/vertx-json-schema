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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String pattern = (String) schema.getValue("pattern");
      return new PatternValidator(Pattern.compile(pattern));
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for pattern keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null pattern keyword", e);
    } catch (PatternSyntaxException e) {
      throw new SchemaException(schema, "Invalid pattern in pattern keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("pattern");
  }

  public class PatternValidator extends BaseSyncValidator {
    private final Pattern pattern;

    public PatternValidator(Pattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof String) {
        Matcher m = pattern.matcher((String) in);
        if (!(m.matches() || m.lookingAt() || m.find())) {
          throw ValidationException.create("provided string should respect pattern " + pattern, "pattern", in);
        }
      }
    }
  }

}
