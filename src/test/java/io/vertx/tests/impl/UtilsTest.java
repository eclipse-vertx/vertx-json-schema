package io.vertx.tests.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.impl.Utils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

  @Test
  public void testEncodeUri() throws URISyntaxException {
      String set1 = ";,/?:@&=+$";  // Reserved Characters
      String set2 = "-_.!~*'()";   // Unescaped Characters
      String set3 = "#";           // Number Sign
      String set4 = "ABC abc 123"; // Alphanumeric Characters + Space

      assertThat(Utils.Pointers.encode(set1))
        .isEqualTo(";,~1?:@&=+$"); // note the escape

    assertThat(Utils.Pointers.encode(set2))
      .isEqualTo("-_.!~0*'()"); // note the escape

    assertThat(Utils.Pointers.encode(set3))
      .isEqualTo("#");

    assertThat(Utils.Pointers.encode(set4))
      .isEqualTo("ABC%20abc%20123");

    assertThat(Utils.Pointers.encode("^\uD83D\uDC32*$"))
      .isEqualTo("%5E%F0%9F%90%B2*$");

    assertThat(Utils.Pointers.encode("percent%field"))
      .isEqualTo("percent%25field");
  }

  @Test
  public void testCanonicalizeNull() {
    assertThat(Utils.JSON.canonicalize(null)).isNull();
  }

  @Test
  public void testCanonicalizeBooleansDistinctFromNumbers() {
    assertThat(Utils.JSON.canonicalize(false)).isNotEqualTo(Utils.JSON.canonicalize(0));
    assertThat(Utils.JSON.canonicalize(true)).isNotEqualTo(Utils.JSON.canonicalize(1));
  }

  @Test
  public void testCanonicalizeNumericEquivalence() {
    Object fromInt = Utils.JSON.canonicalize(1);
    Object fromLong = Utils.JSON.canonicalize(1L);
    Object fromDouble = Utils.JSON.canonicalize(1.0);
    Object fromFloat = Utils.JSON.canonicalize(1.0f);
    Object fromBigDecimal = Utils.JSON.canonicalize(new BigDecimal("1.00"));
    Object fromBigInteger = Utils.JSON.canonicalize(BigInteger.ONE);

    assertThat(fromInt).isEqualTo(fromLong);
    assertThat(fromInt).isEqualTo(fromDouble);
    assertThat(fromInt).isEqualTo(fromFloat);
    assertThat(fromInt).isEqualTo(fromBigDecimal);
    assertThat(fromInt).isEqualTo(fromBigInteger);

    assertThat(fromInt.hashCode()).isEqualTo(fromLong.hashCode());
    assertThat(fromInt.hashCode()).isEqualTo(fromDouble.hashCode());
    assertThat(fromInt.hashCode()).isEqualTo(fromBigDecimal.hashCode());
  }

  @Test
  public void testCanonicalizeNumericNegativeScale() {
    Object a = Utils.JSON.canonicalize(1000);
    Object b = Utils.JSON.canonicalize(new BigDecimal("1000.0"));
    Object c = Utils.JSON.canonicalize(1000L);

    assertThat(a).isEqualTo(b);
    assertThat(a).isEqualTo(c);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  public void testCanonicalizeStrings() {
    assertThat(Utils.JSON.canonicalize("abc")).isEqualTo(Utils.JSON.canonicalize("abc"));
    assertThat(Utils.JSON.canonicalize("abc")).isNotEqualTo(Utils.JSON.canonicalize("def"));
  }

  @Test
  public void testCanonicalizeJsonObjectKeyOrder() {
    JsonObject a = new JsonObject().put("x", 1).put("y", 2);
    JsonObject b = new JsonObject().put("y", 2).put("x", 1);

    Object ca = Utils.JSON.canonicalize(a);
    Object cb = Utils.JSON.canonicalize(b);

    assertThat(ca).isEqualTo(cb);
    assertThat(ca.hashCode()).isEqualTo(cb.hashCode());
  }

  @Test
  public void testCanonicalizeJsonObjectDifferentValues() {
    JsonObject a = new JsonObject().put("x", 1);
    JsonObject b = new JsonObject().put("x", 2);

    assertThat(Utils.JSON.canonicalize(a)).isNotEqualTo(Utils.JSON.canonicalize(b));
  }

  @Test
  public void testCanonicalizeJsonArrayOrder() {
    JsonArray a = new JsonArray().add(1).add(2);
    JsonArray b = new JsonArray().add(2).add(1);

    assertThat(Utils.JSON.canonicalize(a)).isNotEqualTo(Utils.JSON.canonicalize(b));
  }

  @Test
  public void testCanonicalizeJsonArrayEquality() {
    JsonArray a = new JsonArray().add(1).add("two");
    JsonArray b = new JsonArray().add(1).add("two");

    Object ca = Utils.JSON.canonicalize(a);
    Object cb = Utils.JSON.canonicalize(b);

    assertThat(ca).isEqualTo(cb);
    assertThat(ca.hashCode()).isEqualTo(cb.hashCode());
  }

  @Test
  public void testCanonicalizeNestedStructures() {
    JsonObject inner1 = new JsonObject().put("a", 1.0);
    JsonObject inner2 = new JsonObject().put("a", 1);

    JsonArray a = new JsonArray().add(inner1);
    JsonArray b = new JsonArray().add(inner2);

    Object ca = Utils.JSON.canonicalize(a);
    Object cb = Utils.JSON.canonicalize(b);

    assertThat(ca).isEqualTo(cb);
    assertThat(ca.hashCode()).isEqualTo(cb.hashCode());
  }

  @Test
  public void testCanonicalizeArrayBooleanVsNumber() {
    JsonArray a = new JsonArray().add(true);
    JsonArray b = new JsonArray().add(1);

    assertThat(Utils.JSON.canonicalize(a)).isNotEqualTo(Utils.JSON.canonicalize(b));
  }
}
