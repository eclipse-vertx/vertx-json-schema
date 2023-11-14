package io.vertx.json.schema.impl;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class Format {

  public static boolean fastFormat(String format, String value) {
    switch (format) {
      case "byte":
        return testByte(value);
      case "date":
        return testDate(value);
      case "time":
        return testTime(value);
      case "date-time":
        return testDateTime(value);
      case "duration":
        return testDuration(value);
      case "uri":
        return testUri(value);
      case "uri-reference":
        return testUriReference(value);
      case "uri-template":
        return testUriTemplate(value);
      case "url":
        return testUrl(value);
      case "email":
        return testEmail(value);
      case "hostname":
        return testHostname(value);
      case "ipv4":
        return testIpv4(value);
      case "ipv6":
        return testIpv6(value);
      case "regex":
        return testRegex(value);
      case "uuid":
        return testUuid(value);
      case "json-pointer":
        return testJsonPointer(value);
      case "json-pointer-uri-fragment":
        return testJsonPointerUriFragment(value);
      case "relative-json-pointer":
        return testRelativeJsonPointer(value);
      default:
        // unknown formats are assumed true, e.g.: idn-hostname, binary
        return true;
    }
  }

  private static final Pattern BASE64 = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A" +
    "-Za-z0-9+/]{2}==)$");

  private static boolean testByte(String value) {
    return BASE64.matcher(value).find();
  }

  private static final Pattern RELATIVE_JSON_POINTER = Pattern.compile("^(?:0|[1-9][0-9]*)(?:#|(?:\\/(?:[^~/]|~0|~1)" +
    "*)*)$");

  private static boolean testRelativeJsonPointer(String value) {
    return RELATIVE_JSON_POINTER.matcher(value).find();
  }

  private static final Pattern JSON_POINTER_URI_FRAGMENT = Pattern.compile("^#(?:\\/(?:[a-z0-9_\\-.!$&'()*+,;" +
    ":=@]|%[0-9a-f]{2}|~0|~1)*)*$", Pattern.CASE_INSENSITIVE);

  private static boolean testJsonPointerUriFragment(String value) {
    return JSON_POINTER_URI_FRAGMENT.matcher(value).find();
  }

  private static final Pattern JSON_POINTER = Pattern.compile("^(?:\\/(?:[^~/]|~0|~1)*)*$");

  private static boolean testJsonPointer(String value) {
    return JSON_POINTER.matcher(value).find();
  }

  private static final Pattern UUID = Pattern.compile("^(?:urn:uuid:)?[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

  private static boolean testUuid(String value) {
    return UUID.matcher(value).find();
  }

  private static final Pattern Z_ANCHOR = Pattern.compile("[^\\\\]\\\\Z");

  private static boolean testRegex(String value) {
    if (Z_ANCHOR.matcher(value).find()) {
      return false;
    }
    try {
      Pattern.compile(value);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  // optimized http://stackoverflow.com/questions/53497/regular-expression-that-matches-valid-ipv6-addresses
  private static final Pattern IPV6 = Pattern.compile("^((([0-9a-f]{1,4}:){7}([0-9a-f]{1,4}|:))|(([0-9a-f]{1,4}:){6}(:[0-9a-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){5}(((:[0-9a-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){4}(((:[0-9a-f]{1,4}){1,3})|((:[0-9a-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){3}(((:[0-9a-f]{1,4}){1,4})|((:[0-9a-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){2}(((:[0-9a-f]{1,4}){1,5})|((:[0-9a-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){1}(((:[0-9a-f]{1,4}){1,6})|((:[0-9a-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9a-f]{1,4}){1,7})|((:[0-9a-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))$", Pattern.CASE_INSENSITIVE);

  private static boolean testIpv6(String value) {
    return IPV6.matcher(value).find();
  }

  // optimized https://www.safaribooksonline.com/library/view/regular-expressions-cookbook/9780596802837/ch07s16.html
  private static final Pattern IPV4 = Pattern.compile("^(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

  private static boolean testIpv4(String value) {
    return IPV4.matcher(value).find();
  }

  private static final Pattern HOSTNAME = Pattern.compile("^(?=.{1,253}\\.?$)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[-0-9a-z]{0,61}[0-9a-z])?)*\\.?$", Pattern.CASE_INSENSITIVE);

  private static boolean testHostname(String value) {
    return HOSTNAME.matcher(value).find();
  }

  private static final Pattern EMAIL_HOST = Pattern.compile("^[a-z0-9.-]+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EMAIL_NAME = Pattern.compile("^[a-z0-9.!#$%&'*+/=?^_`\\{|\\}~-]+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EMAIL_HOST_PART = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$", Pattern.CASE_INSENSITIVE);

  // https://github.com/ExodusMovement/schemasafe/blob/master/src/formats.js
  private static boolean testEmail(String value) {
     if (value.charAt(0) == '"') {
       return false;
     }
     final String[] parts = value.split("@");
     if (
       parts.length != 2 ||
         Utils.Strings.empty(parts[0]) ||
         Utils.Strings.empty(parts[1]) ||
         parts[0].length() > 64 ||
         parts[1].length() > 253
     ) {
       return false;
     }
     if (parts[0].charAt(0) == '.' || parts[0].endsWith(".") || parts[0].contains("..")) {
       return false;
     }
     if (
       !EMAIL_HOST.matcher(parts[1]).find() ||
       !EMAIL_NAME.matcher(parts[0]).find()
     ) {
       return false;
     }
     for (String part : parts[1].split("\\.")) {
       if (!EMAIL_HOST_PART.matcher(part).find()) {
         return false;
       }
     }
    return true;
  }

  // For the source: https://gist.github.com/dperini/729294
  // For test cases: https://mathiasbynens.be/demo/url-regex
  private static final Pattern URL_ = Pattern.compile("^(?:(?:https?|ftp):\\/\\/)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private static boolean testUrl(String value) {
    return URL_.matcher(value).find();
  }

  // uri-template: https://tools.ietf.org/html/rfc6570
  private static final Pattern URITEMPLATE = Pattern.compile("^(?:(?:[^\\x00-\\x20\"'<>%\\\\^`\\{|\\}]|%[0-9a-f]{2})|\\{[+#./;?&=,!@|]?(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?(?:,(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?)*\\})*$", Pattern.CASE_INSENSITIVE);

  private static boolean testUriTemplate(String value) {
    return URITEMPLATE.matcher(value).find();
  }

  private static final Pattern URIREF = Pattern.compile("^(?:[a-z][a-z0-9+\\-.]*:)?(?:\\/?\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:]|%[0-9a-f]{2})*@)?(?:\\[(?:(?:(?:(?:[0-9a-f]{1,4}:){6}|::(?:[0-9a-f]{1,4}:){5}|(?:[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){4}|(?:(?:[0-9a-f]{1,4}:){0,1}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){3}|(?:(?:[0-9a-f]{1,4}:){0,2}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){2}|(?:(?:[0-9a-f]{1,4}:){0,3}[0-9a-f]{1,4})?::[0-9a-f]{1,4}:|(?:(?:[0-9a-f]{1,4}:){0,4}[0-9a-f]{1,4})?::)(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?))|(?:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})?::[0-9a-f]{1,4}|(?:(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4})?::)|[Vv][0-9a-f]+\\.[a-z0-9\\-._~!$&'()*+,;=:]+)\\]|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)|(?:[a-z0-9\\-._~!$&'\"()*+,;=]|%[0-9a-f]{2})*)(?::\\d*)?(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*|\\/(?:(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*)?|(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*)?(?:\\?(?:[a-z0-9\\-._~!$&'\"()*+,;=:@/?]|%[0-9a-f]{2})*)?(?:#(?:[a-z0-9\\-._~!$&'\"()*+,;=:@/?]|%[0-9a-f]{2})*)?$", Pattern.CASE_INSENSITIVE);

  private static boolean testUriReference(String value) {
    return URIREF.matcher(value).find();
  }

  private static final Pattern NOT_URI_FRAGMENT = Pattern.compile("\\/|:");
  private static final Pattern URI_PATTERN = Pattern.compile("^(?:[a-z][a-z0-9+\\-.]*:)(?:\\/?\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:]|%[0-9a-f]{2})*@)?(?:\\[(?:(?:(?:(?:[0-9a-f]{1,4}:){6}|::(?:[0-9a-f]{1,4}:){5}|(?:[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){4}|(?:(?:[0-9a-f]{1,4}:){0,1}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){3}|(?:(?:[0-9a-f]{1,4}:){0,2}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){2}|(?:(?:[0-9a-f]{1,4}:){0,3}[0-9a-f]{1,4})?::[0-9a-f]{1,4}:|(?:(?:[0-9a-f]{1,4}:){0,4}[0-9a-f]{1,4})?::)(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?))|(?:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})?::[0-9a-f]{1,4}|(?:(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4})?::)|[Vv][0-9a-f]+\\.[a-z0-9\\-._~!$&'()*+,;=:]+)\\]|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)|(?:[a-z0-9\\-._~!$&'()*+,;=]|%[0-9a-f]{2})*)(?::\\d*)?(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*|\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*)?|(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*)(?:\\?(?:[a-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9a-f]{2})*)?(?:#(?:[a-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9a-f]{2})*)?$", Pattern.CASE_INSENSITIVE);

  private static boolean testUri(String value) {
    // http://jmrware.com/articles/2009/uri_regexp/URI_regex.html + optional protocol + required "."
    return NOT_URI_FRAGMENT.matcher(value).find() && URI_PATTERN.matcher(value).find();
  }

  private static final Pattern DURATION_A = Pattern.compile("^-?P\\d+([.,]\\d+)?W$");
  private static final Pattern DURATIION_B = Pattern.compile("^-?P[\\dYMDTHS]*(\\d[.,]\\d+)?[YMDHS]$");
  private static final Pattern DURATIION_C = Pattern.compile("^-?P([.,\\d]+Y)?([.,\\d]+M)?([.,\\d]+D)?(T([.,\\d]+H)?([.,\\d]+M)?([.,\\d]+S)?)?$");

  private static boolean testDuration(String value) {
    return value.length() > 1 &&
      value.length() < 80 &&
      (DURATION_A.matcher(value).find() ||
        (DURATIION_B.matcher(value).find() &&
          DURATIION_C.matcher(value).find()));
  }

  private static final Pattern FASTDATETIME = Pattern.compile("^\\d\\d\\d\\d-[0-1]\\d-[0-3]\\d[t\\s](?:[0-2]\\d:[0-5]\\d:[0-5]\\d|23:59:60)(?:\\.\\d+)?(?:z|[+-]\\d\\d(?::?\\d\\d)?)$", Pattern.CASE_INSENSITIVE);

  private static boolean testDateTime(String value) {
    return FASTDATETIME.matcher(value).find();
  }

  // date-time: http://tools.ietf.org/html/rfc3339#section-5.6
  private static final Pattern FASTTIME = Pattern.compile("^(?:[0-2]\\d:[0-5]\\d:[0-5]\\d|23:59:60)(?:\\.\\d+)?(?:z|[+-]\\d\\d(?::?\\d\\d)?)?$", Pattern.CASE_INSENSITIVE);

  private static boolean testTime(String value) {
    return FASTTIME.matcher(value).find();
  }

  // date: http://tools.ietf.org/html/rfc3339#section-5.6
  private static final Pattern FASTDATE = Pattern.compile("^\\d\\d\\d\\d-[0-1]\\d-[0-3]\\d$");

  private static boolean testDate(String value) {
    if (!FASTDATE.matcher(value).matches()) {
      return false;
    }
    try {
      DateTimeFormatter.ISO_DATE.parse(value);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
