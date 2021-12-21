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

import java.net.IDN;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class BaseFormatValidatorFactory implements ValidatorFactory {

  protected final static Predicate<String> URI_VALIDATOR = in -> {
    try {
      return URI.create(in).isAbsolute();
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> URI_REFERENCE_VALIDATOR = in -> {
    try {
      URI.create(in);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> REGEX_VALIDATOR = in -> {
    try {
      Pattern.compile(in);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  };

  protected final static Predicate<String> IDN_HOSTNAME_VALIDATOR = in -> {
    try {
      IDN.toASCII(in);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> IDN_EMAIL_VALIDATOR = in -> {
    try {
      int atIndex = in.indexOf('@');
      if (atIndex < 0) {
        return false;
      }
      String localPart = in.substring(0, atIndex);
      if (!RegularExpressions.EMAIL_LOCAL.matcher(localPart).matches()) {
        return false;
      }
      IDN.toASCII(in.substring(atIndex + 1));
      return true;
    } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
      return false;
    }
  };

  protected final static Predicate<String> UUID_VALIDATOR = in -> {
    try {
      UUID.fromString(in);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  };

  protected final static Predicate<String> DURATION_VALIDATOR = in -> {
    try {
      Duration.parse(in);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  };

  static class FormatValidator extends BaseSyncValidator {

    final Predicate<String> validator;

    public FormatValidator(Predicate<String> validator) {
      this.validator = validator;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof String) {
        if (!validator.test((String) in)) {
          throw ValidationException.create("Provided value don't match pattern", "pattern", in);
        }
      }
    }
  }

  protected final Map<String, Predicate<String>> formats;
  protected final List<String> ignoringFormats;

  public BaseFormatValidatorFactory() {
    this.formats = initFormatsMap();
    this.ignoringFormats = initIgnoringFormats();
  }

  protected List<String> initIgnoringFormats() {
    return Arrays.asList(
      "int32",
      "int64",
      "float",
      "double"
    );
  }

  public abstract Map<String, Predicate<String>> initFormatsMap();

  public void addStringFormatValidator(String formatName, Predicate<String> validator) {
    this.formats.put(formatName, validator);
  }

  protected Predicate<String> createPredicateFromPattern(final Pattern pattern) {
    return (in) -> pattern.matcher(in).matches();
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    String format = schema.getString("format");
    if (ignoringFormats.contains(format)) return null;
    else {
      Predicate<String> v = formats.get(format);
      if (v == null) throw new SchemaException(schema, "Format not supported");
      else return new FormatValidator(v);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("format");
  }
}
