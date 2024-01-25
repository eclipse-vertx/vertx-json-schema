package io.vertx.json.schema.impl;

import org.junit.jupiter.api.Test;

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
  public void testIsSmallerOrEqualToThanInteger() {
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Byte.MAX_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Byte.MIN_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Short.MAX_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Short.MIN_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Integer.MAX_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Integer.MIN_VALUE)).isTrue();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Long.MAX_VALUE)).isFalse();
    assertThat(Utils.Numbers.isSmallerOrEqualToThanInteger(Long.MIN_VALUE)).isFalse();
  }
}
