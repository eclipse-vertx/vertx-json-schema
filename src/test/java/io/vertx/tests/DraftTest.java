package io.vertx.tests;

import io.vertx.json.schema.Draft;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DraftTest {

  public Stream<Arguments> getDraftOrders() {
    return Stream.of(
      Arguments.of(Draft.DRAFT4, Draft.DRAFT7, false),
      Arguments.of(Draft.DRAFT4, Draft.DRAFT201909, false),
      Arguments.of(Draft.DRAFT4, Draft.DRAFT202012, false),
      Arguments.of(Draft.DRAFT7, Draft.DRAFT201909, false),
      Arguments.of(Draft.DRAFT7, Draft.DRAFT202012, false),
      Arguments.of(Draft.DRAFT201909, Draft.DRAFT202012, false)
      );
  }

  @ParameterizedTest
  @MethodSource("getDraftOrders")
  public void testDraftOrders(Draft d1, Draft d2, boolean after) {
    assertThat(d1.isAfter(d2)).isEqualTo(after);
    assertThat(d1.isBefore(d2)).isEqualTo(!after);
  }

}
