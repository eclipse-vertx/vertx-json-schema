package io.vertx.ext.json.schema.draft7;

import io.vertx.core.json.pointer.impl.JsonPointerImpl;
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
    predicates.put("uri-reference", URI_REFERENCE_VALIDATOR);
    predicates.put("regex", REGEX_VALIDATOR);
    predicates.put("json-pointer", createPredicateFromPattern(JsonPointerImpl.VALID_POINTER_PATTERN));
    predicates.put("relative-json-pointer", createPredicateFromPattern(RegularExpressions.RELATIVE_JSON_POINTER));
    predicates.put("uri-template", createPredicateFromPattern(RegularExpressions.URI_TEMPLATE));
    predicates.put("time", createPredicateFromPattern(RegularExpressions.TIME));
    return predicates;
  }
}
