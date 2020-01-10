package io.vertx.ext.json.schema.openapi3;

import io.vertx.ext.json.schema.common.BaseFormatValidatorFactory;
import io.vertx.ext.json.schema.common.RegularExpressions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FormatValidatorFactory extends BaseFormatValidatorFactory {
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
