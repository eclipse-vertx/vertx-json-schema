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
package io.vertx.ext.json.schema.openapi3;

import io.vertx.ext.json.schema.common.BaseFormatValidatorFactory;
import io.vertx.ext.json.schema.common.RegularExpressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FormatValidatorFactory extends BaseFormatValidatorFactory {

  @Override
  protected List<String> initIgnoringFormats() {
    List<String> formats = new ArrayList<>(super.initIgnoringFormats());
    formats.add("password");
    return formats;
  }

  @Override
  public Map<String, Predicate<String>> initFormatsMap() {
    Map<String, Predicate<String>> predicates = new HashMap<>();
    predicates.put("byte", createPredicateFromPattern(RegularExpressions.BASE64));
    predicates.put("date", createPredicateFromPattern(RegularExpressions.DATE));
    predicates.put("date-time", createPredicateFromPattern(RegularExpressions.DATETIME));
    predicates.put("ipv4", createPredicateFromPattern(RegularExpressions.IPV4));
    predicates.put("ipv6", createPredicateFromPattern(RegularExpressions.IPV6));
    predicates.put("hostname", createPredicateFromPattern(RegularExpressions.HOSTNAME));
    predicates.put("email", createPredicateFromPattern(RegularExpressions.EMAIL));
    predicates.put("uri", URI_VALIDATOR);
    predicates.put("uriref", URI_REFERENCE_VALIDATOR);
    return predicates;
  }
}
