package io.vertx.json.schema.impl;

import java.net.IDN;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class Format {

  private static final Pattern BASE16 = Pattern.compile("[0-9a-f]", CASE_INSENSITIVE);
  private static final Pattern BASE32 = Pattern.compile("^(?:[A-Z2-7]{8})*(?:[A-Z2-7]{2}={6}|[A-Z2-7]{4}={4}|[A-Z2-7]{5}={3}|[A-Z2-7]{7}=)?$");
  private static final Pattern BASE64 = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A" +
    "-Za-z0-9+/]{2}==)$");
  private static final Pattern RELATIVE_JSON_POINTER = Pattern.compile("^(?:0|[1-9][0-9]*)(?:#|(?:\\/(?:[^~/]|~0|~1)" +
    "*)*)$");
  private static final Pattern JSON_POINTER_URI_FRAGMENT = Pattern.compile("^#(?:\\/(?:[a-z0-9_\\-.!$&'()*+,;" +
    ":=@]|%[0-9a-f]{2}|~0|~1)*)*$", CASE_INSENSITIVE);
  private static final Pattern JSON_POINTER = Pattern.compile("^(?:\\/(?:[^~/]|~0|~1)*)*$");
  private static final Pattern UUID = Pattern.compile("^(?:urn:uuid:)?[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$", CASE_INSENSITIVE);
  private static final Pattern Z_ANCHOR = Pattern.compile("[^\\\\]\\\\Z");
  // optimized http://stackoverflow.com/questions/53497/regular-expression-that-matches-valid-ipv6-addresses
  private static final Pattern IPV6 = Pattern.compile("^((([0-9a-f]{1,4}:){7}([0-9a-f]{1,4}|:))|(([0-9a-f]{1,4}:){6}(:[0-9a-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){5}(((:[0-9a-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9a-f]{1,4}:){4}(((:[0-9a-f]{1,4}){1,3})|((:[0-9a-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){3}(((:[0-9a-f]{1,4}){1,4})|((:[0-9a-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){2}(((:[0-9a-f]{1,4}){1,5})|((:[0-9a-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9a-f]{1,4}:){1}(((:[0-9a-f]{1,4}){1,6})|((:[0-9a-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9a-f]{1,4}){1,7})|((:[0-9a-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))$", CASE_INSENSITIVE);

  // optimized https://www.safaribooksonline.com/library/view/regular-expressions-cookbook/9780596802837/ch07s16.html
  private static final Pattern IPV4 = Pattern.compile("^(?:(?:25[0-5]|2[0-4]\\d|[1]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
  private static final Pattern HOSTNAME = Pattern.compile("^(?=.{1,253}\\.?$)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[-0-9a-z]{0,61}[0-9a-z])?)*\\.?$", CASE_INSENSITIVE);

  // https://github.com/ExodusMovement/schemasafe/blob/master/src/formats.js
  private static final Pattern EMAIL_HOST = Pattern.compile("^[a-z0-9.-]+$", CASE_INSENSITIVE);
  private static final Pattern EMAIL_NAME = Pattern.compile("^[a-z0-9.!#$%&'*+/=?^_`\\{|\\}~-]+$", CASE_INSENSITIVE);
  private static final Pattern EMAIL_HOST_PART = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$", CASE_INSENSITIVE);
  // For the source: https://gist.github.com/dperini/729294
  // For test cases: https://mathiasbynens.be/demo/url-regex
  private static final Pattern URL_ = Pattern.compile("^(?:(?:https?|ftp):\\/\\/)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?$", CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  //IDN emails can have - and . throughout unlike a normal email address. The email host part can also have the same.
  private static final Pattern IDN_EMAIL_PUNY = Pattern.compile("^xn--[a-z0-9-.]*@[a-z0-9-.]*$");
  private static final Pattern IDN_EMAIL_HOST = Pattern.compile("^[a-z0-9-.]*");
  private static final Pattern IDN_EMAIL_NAME = Pattern.compile("^xn--[a-z0-9-.]*$");
  private static final Pattern IDN_EMAIL_HOST_PART = Pattern.compile("^[a-z0-9-]([a-z0-9-]{0,61}[a-z0-9-])?$");
  private static final Pattern NOT_URI_FRAGMENT = Pattern.compile("\\/|:");
  private static final Pattern URI_PATTERN = Pattern.compile("^(?:[a-z][a-z0-9+\\-.]*:)(?:\\/?\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:]|%[0-9a-f]{2})*@)?(?:\\[(?:(?:(?:(?:[0-9a-f]{1,4}:){6}|::(?:[0-9a-f]{1,4}:){5}|(?:[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){4}|(?:(?:[0-9a-f]{1,4}:){0,1}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){3}|(?:(?:[0-9a-f]{1,4}:){0,2}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){2}|(?:(?:[0-9a-f]{1,4}:){0,3}[0-9a-f]{1,4})?::[0-9a-f]{1,4}:|(?:(?:[0-9a-f]{1,4}:){0,4}[0-9a-f]{1,4})?::)(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?))|(?:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})?::[0-9a-f]{1,4}|(?:(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4})?::)|[Vv][0-9a-f]+\\.[a-z0-9\\-._~!$&'()*+,;=:]+)\\]|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)|(?:[a-z0-9\\-._~!$&'()*+,;=]|%[0-9a-f]{2})*)(?::\\d*)?(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*|\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*)?|(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2})*)*)(?:\\?(?:[a-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9a-f]{2})*)?(?:#(?:[a-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9a-f]{2})*)?$", CASE_INSENSITIVE);
  private static final Pattern URIREF = Pattern.compile("^(?:[a-z][a-z0-9+\\-.]*:)?(?:\\/?\\/(?:(?:[a-z0-9\\-._~!$&'()*+,;=:]|%[0-9a-f]{2})*@)?(?:\\[(?:(?:(?:(?:[0-9a-f]{1,4}:){6}|::(?:[0-9a-f]{1,4}:){5}|(?:[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){4}|(?:(?:[0-9a-f]{1,4}:){0,1}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){3}|(?:(?:[0-9a-f]{1,4}:){0,2}[0-9a-f]{1,4})?::(?:[0-9a-f]{1,4}:){2}|(?:(?:[0-9a-f]{1,4}:){0,3}[0-9a-f]{1,4})?::[0-9a-f]{1,4}:|(?:(?:[0-9a-f]{1,4}:){0,4}[0-9a-f]{1,4})?::)(?:[0-9a-f]{1,4}:[0-9a-f]{1,4}|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?))|(?:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})?::[0-9a-f]{1,4}|(?:(?:[0-9a-f]{1,4}:){0,6}[0-9a-f]{1,4})?::)|[Vv][0-9a-f]+\\.[a-z0-9\\-._~!$&'()*+,;=:]+)\\]|(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)|(?:[a-z0-9\\-._~!$&'\"()*+,;=]|%[0-9a-f]{2})*)(?::\\d*)?(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*|\\/(?:(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*)?|(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})+(?:\\/(?:[a-z0-9\\-._~!$&'\"()*+,;=:@]|%[0-9a-f]{2})*)*)?(?:\\?(?:[a-z0-9\\-._~!$&'\"()*+,;=:@/?]|%[0-9a-f]{2})*)?(?:#(?:[a-z0-9\\-._~!$&'\"()*+,;=:@/?]|%[0-9a-f]{2})*)?$", CASE_INSENSITIVE);
  // uri-template: https://tools.ietf.org/html/rfc6570
  private static final Pattern URITEMPLATE = Pattern.compile("^(?:(?:[^\\x00-\\x20\"'<>%\\\\^`\\{|\\}]|%[0-9a-f]{2})|\\{[+#./;?&=,!@|]?(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?(?:,(?:[a-z0-9_]|%[0-9a-f]{2})+(?::[1-9][0-9]{0,3}|\\*)?)*\\})*$", CASE_INSENSITIVE);
  private static final Pattern DURATION_A = Pattern.compile("^P\\d+([.,]\\d+)?W$");
  private static final Pattern DURATION_B = Pattern.compile("^P[\\dYMDTHS]*(\\d[.,]\\d+)?[YMDHS]$");
  private static final Pattern DURATION_C = Pattern.compile("^P([.,\\d]+Y)?([.,\\d]+M)?([.,\\d]+D)?(T([.,\\d]+H)?([.,\\d]+M)?([.,\\d]+S)?)?$");
  private static final Pattern FASTDATETIME = Pattern.compile("^\\d\\d\\d\\d-[0-1]\\d-[0-3]\\d[t\\s](?:[0-2]\\d:[0-5]\\d:[0-5]\\d|23:59:60)(?:\\.\\d+)?(?:z?|[+-]\\d\\d(?::?\\d\\d)?)$", CASE_INSENSITIVE);
  //IDN Puny code is only ever in this format. xn--abc.xyz
  private static final Pattern IDN_HOSTNAME_PUNY = Pattern.compile("^xn--[a-z0-9-.]*$");

  // This is checking various other combinations of invalid characters/combinations of invalid characters.
  private static final Pattern IDN_HOSTNAME_UNICODE = Pattern
    .compile("^.*\\u302e.*|(^.*?[^l]\\u00b7.|.*l\\u00b7[^l]|\\u00b7$|^\\u00b7.)|(.*\\u30fB[^\\u3041\\u30A1\\u4e08].*|^\\u30fB$)|(^[\\u05f3\\u05f4].*)$"
      , Pattern.UNICODE_CHARACTER_CLASS);

  // This is checking the start of the word for Mc,Me or Mn characters.
  // Mc -> Spacing_Combining_Mark
  // Me -> Enclosing_Mark
  // Mn -> Non_Spacing_Mark (excluding if Virma \\u094d follows
  private static final Pattern IDN_HOSTNAME_STARTING_ERRORS = Pattern
    .compile("^\\p{gc=Mc}|^\\p{gc=Me}|^\\p{gc=Mn}(?!\\u094d)", Pattern.UNICODE_CHARACTER_CLASS);
  // date-time: http://tools.ietf.org/html/rfc3339#section-5.6
  private static final Pattern FASTTIME = Pattern.compile("^(?:[0-2]\\d:[0-5]\\d:[0-5]\\d|23:59:60)(?:\\.\\d+)?(?:z|[+-]\\d\\d(?::?\\d\\d)?)?$", CASE_INSENSITIVE);
  private static final Pattern VALID_LEAP_SECONDS = Pattern.compile("^23:59:60(?:\\.\\d+)?(?:z|[+-]00(?::?00)?)?$", CASE_INSENSITIVE);

  // date: http://tools.ietf.org/html/rfc3339#section-5.6
  private static final Pattern FASTDATE = Pattern.compile("^\\d\\d\\d\\d-[0-1]\\d-[0-3]\\d$");

  public static boolean fastFormat(String format, String value) {
    switch (format) {
      case "byte":
        return checkPattern(value, BASE64);
      case "date":
        return testDate(value);
      case "time":
        return testTime(value);
      case "date-time":
        return testDateTime(value);
      case "duration":
        return testDuration(value);
      case "uri":
        // http://jmrware.com/articles/2009/uri_regexp/URI_regex.html + optional protocol + required "."
        return checkPattern(value, NOT_URI_FRAGMENT) && checkPattern(value, URI_PATTERN);
      case "uri-reference":
        return checkPattern(value, URIREF);
      case "uri-template":
        return checkPattern(value, URITEMPLATE);
      case "url":
        return checkPattern(value,URL_);
      case "email":
        return testEmail(value);
      case "hostname":
        return checkPattern(value, HOSTNAME);
      case "ipv4":
        return checkPattern(value, IPV4);
      case "ipv6":
        return checkPattern(value, IPV6);
      case "regex":
        return testRegex(value);
      case "uuid":
        return checkPattern(value, UUID);
      case "json-pointer":
        return checkPattern(value, JSON_POINTER);
      case "json-pointer-uri-fragment":
        return checkPattern(value, JSON_POINTER_URI_FRAGMENT);
      case "relative-json-pointer":
        return checkPattern(value, RELATIVE_JSON_POINTER);
      case "idn-hostname":
        return testIdnHostname(value);
      case "idn-email":
        return testIdnEmail(value);
      default:
        // unknown formats are assumed true, e.g.: idn-hostname, binary
        return true;
    }
  }

  public static boolean testContentEncoding(String format, String value) {
    switch(format) {
      case "base64":
        return checkPattern(value, BASE64);
      case "base32":
        return checkPattern(value, BASE32);
      case "base16":
        return checkPattern(value, BASE16);
      default:
        return true;
    }
  }

  private static boolean checkPattern(String value, Pattern p) {
    return p.matcher(value).find();
  }

  private static boolean testRegex(String value) {
    if (checkPattern(value, Z_ANCHOR)) {
      return false;
    }
    try {
      Pattern.compile(value);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean testEmail(String value) {
    return testEmail(value, EMAIL_HOST, EMAIL_NAME, EMAIL_HOST_PART);
  }

  private static boolean testEmail(String value, Pattern emailHostMatcher, Pattern emailNameMatcher, Pattern emailHostPartMatcher) {
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
      !checkPattern( parts[1], emailHostMatcher)||
        !checkPattern(parts[0], emailNameMatcher)
    ) {
      return false;
    }
    for (String part : parts[1].split("\\.")) {
      if (!checkPattern(part, emailHostPartMatcher)) {
        return false;
      }
    }
    return true;
  }

  private static boolean testDuration(String value) {
    return value.length() > 1 &&
      value.length() < 80 &&
      (checkPattern(value, DURATION_A) ||
        (checkPattern(value, DURATION_B) &&
          checkPattern(value, DURATION_C)));
  }

  private static boolean testDateTime(String value) {
    if(!checkPattern(value, FASTDATETIME)) {
      return false;
    }
    return validateISOTime(DateTimeFormatter.ISO_DATE_TIME, value);
  }

  private static boolean testTime(String value) {
    if(!checkPattern(value, FASTTIME)) {
      return false;
    }

    //The built-in ISO_TIME does not account for leap seconds.
    // So if it IS a leap second (or matches) just assume that it's OK
    if(checkPattern(value, VALID_LEAP_SECONDS)) {
      return true;
    }

    return validateISOTime(DateTimeFormatter.ISO_TIME, value);
  }

  private static boolean testDate(String value) {
    if (!checkPattern(value, FASTDATE)) {
      return false;
    }

    return validateISOTime(DateTimeFormatter.ISO_DATE, value);
  }

  private static boolean validateISOTime(DateTimeFormatter formatter, String value) {
    try {
      formatter.parse(value);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private static boolean testIdnHostname(String value) {
    try {
      return !checkPattern(value, IDN_HOSTNAME_STARTING_ERRORS) && !checkPattern(value, IDN_HOSTNAME_UNICODE) && checkPattern(IDN.toASCII(value), IDN_HOSTNAME_PUNY);
    } catch(Exception e) {
      return false;
    }
  }

  private static boolean testIdnEmail(String value) {
    try {
      String asciiVersion = IDN.toASCII(value);
      //we were given a non IDN email, so just check if that email is valid or not. No need to do any other checks.
      if(asciiVersion.equals(value)) {
        return testEmail(asciiVersion);
      }

      return checkPattern(asciiVersion, IDN_EMAIL_PUNY) && testEmail(asciiVersion, IDN_EMAIL_HOST, IDN_EMAIL_NAME, IDN_EMAIL_HOST_PART);
    } catch(Exception e) {
      return false;
    }
  }


}
