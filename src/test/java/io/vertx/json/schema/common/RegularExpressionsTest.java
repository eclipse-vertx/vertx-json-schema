package io.vertx.json.schema.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lcharlois
 * @since 06/10/2023.
 */
class RegularExpressionsTest {

  @ParameterizedTest
  @ValueSource(strings = {"P3Y6M4DT12H30M5S","P3DT12H","P1M","PT1M","PT0S","P0.5Y","PT1M3.025S"}) // six numbers
  void shouldMatchValidDuration(String duration) {
    assertTrue(RegularExpressions.DURATION.matcher(duration).matches());
  }


}
