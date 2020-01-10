package io.vertx.ext.json.schema.common;

import java.util.Objects;

public class ComparisonUtils {

  /*
   In Json Schema there is no difference between different number sizing
   */
  public static boolean equalsNumberSafe(Object a, Object b) {
    if (a instanceof Integer) a = ((Integer)a).longValue();
    if (b instanceof Integer) b = ((Integer)b).longValue();
    if (a instanceof Float) a = ((Float)a).doubleValue();
    if (b instanceof Float) b = ((Float)b).doubleValue();
    return Objects.equals(a, b);
  }

}
