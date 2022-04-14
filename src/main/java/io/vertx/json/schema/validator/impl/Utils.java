package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {

  static class Numbers {
    public static boolean isInteger(Object instance) {
      if (instance instanceof Number) {
        if (instance instanceof Byte || instance instanceof Short || instance instanceof Integer || instance instanceof Long || instance instanceof BigInteger) {
          return true;
        }

        if (instance instanceof Float) {
          return (Float) instance % 1 == 0.0f;
        }

        if (instance instanceof Double) {
          return (Double) instance % 1 == 0.0;
        }

        if (instance instanceof BigDecimal) {
          return ((BigDecimal) instance).remainder(BigDecimal.ONE).equals(BigDecimal.ZERO);
        }
      }
      return false;
    }

    private static BigDecimal toBigDecimal(Number in) {
      if (in instanceof BigDecimal) {
        return (BigDecimal) in;
      }
      if (in instanceof BigInteger) {
        return new BigDecimal((BigInteger) in);
      }
      if (in instanceof Float) {
        return BigDecimal.valueOf(in.floatValue());
      }
      if (in instanceof Double) {
        return BigDecimal.valueOf(in.doubleValue());
      }
      return BigDecimal.valueOf(in.longValue());
    }

    public static boolean lt(Number instance, Number value) {
      // for big numbers, go slow
      if (instance instanceof BigDecimal || value instanceof BigDecimal || instance instanceof BigInteger || value instanceof BigInteger) {
        return toBigDecimal(instance).compareTo(toBigDecimal(value)) < 0;
      }
      // approx.
      return instance.doubleValue() < value.doubleValue();
    }

    public static boolean lte(Number instance, Number value) {
      // for big numbers, go slow
      if (instance instanceof BigDecimal || value instanceof BigDecimal || instance instanceof BigInteger || value instanceof BigInteger) {
        return toBigDecimal(instance).compareTo(toBigDecimal(value)) <= 0;
      }
      // approx.
      return instance.doubleValue() <= value.doubleValue();
    }

    public static boolean gt(Number instance, Number value) {
      // for big numbers, go slow
      if (instance instanceof BigDecimal || value instanceof BigDecimal || instance instanceof BigInteger || value instanceof BigInteger) {
        return toBigDecimal(instance).compareTo(toBigDecimal(value)) > 0;
      }
      // approx.
      return instance.doubleValue() > value.doubleValue();
    }

    public static boolean gte(Number instance, Number value) {
      // for big numbers, go slow
      if (instance instanceof BigDecimal || value instanceof BigDecimal || instance instanceof BigInteger || value instanceof BigInteger) {
        return toBigDecimal(instance).compareTo(toBigDecimal(value)) >= 0;
      }
      // approx.
      return instance.doubleValue() >= value.doubleValue();
    }

    public static double remainder(Number instance, Number value) {
      // for big numbers, go slow
      if (instance instanceof BigDecimal || value instanceof BigDecimal || instance instanceof BigInteger || value instanceof BigInteger) {
        return toBigDecimal(instance).remainder(toBigDecimal(value)).doubleValue();
      }
      // for floating point use double
      if (instance instanceof Double || value instanceof Double || instance instanceof Float || value instanceof Float) {
        return instance.doubleValue() % value.doubleValue();
      }
      // for integer use long
      return instance.longValue() % value.doubleValue();
    }

    public static boolean equals(Number a, Number b) {
      if (a == null || b == null) {
        return false;
      }
      if (isInteger(a) && isInteger(b)) {
        // expensive path
        if (a instanceof BigInteger || b instanceof BigInteger) {
          return toBigDecimal(a).equals(toBigDecimal(b));
        }
        // compute using long
        return a.longValue() == b.longValue();
      }
      if (a instanceof BigDecimal || b instanceof BigDecimal) {
        // expensive path
        return toBigDecimal(a).equals(toBigDecimal(b));
      }
      // approx. with double value
      return a.doubleValue() == b.doubleValue();
    }
  }

  static class Strings {
    public static boolean notEmpty(String string) {
      return string != null && string.length() > 0;
    }

    public static boolean empty(String string) {
      return string == null || string.length() == 0;
    }

    /**
     * Get UCS-2 length of a string
     * https://mathiasbynens.be/notes/javascript-encoding
     * https://github.com/bestiejs/punycode.js - punycode.ucs2.decode
     */
    public static int ucs2length(String s) {
      int result = 0;
      int length = s.length();
      int index = 0;
      char charCode;

      while (index < length) {
        result++;
        charCode = s.charAt(index++);
        if (charCode >= 0xd800 && charCode <= 0xdbff && index < length) {
          // high surrogate, and there is a next character
          charCode = s.charAt(index);
          if ((charCode & 0xfc00) == 0xdc00) {
            // low surrogate
            index++;
          }
        }
      }
      return result;
    }
  }

  static class JSON {

    public static Object jsonify(Object instance) {
      if (instance instanceof Map) {
        return new JsonObject((Map) instance);
      }
      if (instance instanceof List) {
        return new JsonArray((List) instance);
      }
      return instance;
    }

    public static String typeOf(Object instance) {
      if (instance == null) {
        return "null";
      }
      if (instance instanceof Boolean) {
        return "boolean";
      }
      if (instance instanceof Number) {
        return "number";
      }
      if (instance instanceof String) {
        return "string";
      }
      if (instance instanceof JsonObject) {
        return "object";
      }
      if (instance instanceof JsonArray) {
        return "array";
      }
      // nothing can be evaluated for the given instance
      throw new IllegalArgumentException("Instances of " + instance.getClass() + " type are not supported");
    }

    public static boolean deepCompare(Object a, Object b) {
      final String typeofa = typeOf(a);
      if (!typeofa.equals(typeOf(b))) {
        return false;
      }
      if (a instanceof JsonArray) {
        if (!(b instanceof JsonArray)) {
          return false;
        }
        final int length = ((JsonArray) a).size();
        if (length != ((JsonArray) b).size()) {
          return false;
        }
        for (int i = 0; i < length; i++) {
          if (!deepCompare(((JsonArray) a).getValue(i), ((JsonArray) b).getValue(i))) {
            return false;
          }
        }
        return true;
      }
      if ("object".equals(typeofa)) {
        if (a == null || b == null) {
          return a == b;
        }
        final Set<String> aKeys = ((JsonObject) a).fieldNames();
        final Set<String> bKeys = ((JsonObject) b).fieldNames();
        final int length = aKeys.size();
        if (length != bKeys.size()) {
          return false;
        }
        for (String k : aKeys) {
          if (!deepCompare(((JsonObject) a).getValue(k), ((JsonObject) b).getValue(k))) {
            return false;
          }
        }
        return true;
      }

      return Objects.equals(a, b);
    }
  }

  static class Pointers {
    public static String encode(String p) {
      try {
        return URLEncoder.encode(escape(p), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    private static String escape(String p) {
      return p
        .replaceAll("~", "~0")
        .replaceAll("/", "~1");
    }
  }

  static class Objects {

    public static boolean equals(Object a, Object b) {
      if (a == null && b == null) {
        return true;
      }
      if (a == null) {
        return false;
      }
      if (b == null) {
        return false;
      }

      if (a instanceof Number && b instanceof Number) {
        return Numbers.equals((Number) a, (Number) b);
      }

      return a.equals(b);
    }
  }
}
