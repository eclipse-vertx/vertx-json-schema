package io.vertx.json.schema.validator.impl;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class URLTest {

  @Test
  public void test() throws URISyntaxException {
    // Base urls
    String m = "https://developer.mozilla.org";
    URL a = new URL("/", m);
    assertThat(a.href())
      .isEqualTo("https://developer.mozilla.org/");

    URL b = new URL(m);
    assertThat(b.href())
      .isEqualTo("https://developer.mozilla.org/");

    assertThat(new URL("en-US/docs", b).href())
      .isEqualTo("https://developer.mozilla.org/en-US/docs");
    URL d = new URL("/en-US/docs", b);
    assertThat(d.href())
      .isEqualTo("https://developer.mozilla.org/en-US/docs");
    assertThat(new URL("/en-US/docs", d).href())
      .isEqualTo("https://developer.mozilla.org/en-US/docs");
    assertThat(new URL("/en-US/docs", a).href())
      .isEqualTo("https://developer.mozilla.org/en-US/docs");

    assertThat(new URL("/en-US/docs", "https://developer.mozilla.org/fr-FR/toto").href())
      .isEqualTo("https://developer.mozilla.org/en-US/docs");

    try {
      new URL("/en-US/docs", "");
      fail("Raises an exception as '' is not a valid URL");
    } catch (RuntimeException e) {
      // OK
    }
    try {
      new URL("/en-US/docs");
      fail("Raises an exception as /en-US/docs is not a valid URL");
    } catch (RuntimeException e) {
      // OK
    }
    assertThat(new URL("http://www.example.com", (String) null).href())
      .isEqualTo("http://www.example.com/");
    assertThat(new URL("http://www.example.com", b).href())
      .isEqualTo("http://www.example.com/");

    assertThat(new URL("/a", "https://example.com/?query=1").href())
      .isEqualTo("https://example.com/a");        // (see relative URLs)
  }

  @Test
  public void testMergeEmptyWithQueryContext() {
    assertThat(new URL("", "https://example.com/?query=1").href())
      .isEqualTo("https://example.com/?query=1"); // (Edge before 79 removes query arguments)
  }

  @Test
  public void testMergeRelativeDomain() {
    assertThat(new URL("//foo.com", "https://example.com").href())
      .isEqualTo("https://foo.com");              // (see relative URLs)
  }

  @Test
  public void testHashPreserve() {
    assertThat(new URL("http://json-schema.org/draft-04/schema#", "http://json-schema.org/draft-04/schema").href())
      .isEqualTo("http://json-schema.org/draft-04/schema#");

  }
}
