package io.vertx.json.schema.impl;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class URLTest {

  @Test
  public void testBadURLSlash() {
    try {
      new URL("/");
      fail("This is strict mode, scheme and host are required");
    } catch (RuntimeException e) {
      // OK
    }
  }

  @Test
  public void testBadURLOnlyHost() {
    try {
      new URL("www.vertx.io");
      fail("This is strict mode, scheme is required");
    } catch (RuntimeException e) {
      // OK
    }
  }

  @Test
  public void testFileOKWithoutHost() {
    try {
      new URL("file://");
    } catch (RuntimeException e) {
      fail("This is strict mode, however file doesn't require a host");
    }
  }

  @Test
  public void testSchema() {
    URL a = new URL("http://json-schema.org/draft-4/schema#", "http://json-schema.org/draft-4/schema");
    assertThat(a.href())
      .isEqualTo("http://json-schema.org/draft-4/schema#");
  }

  @Test
  public void testHrefAddsSlash() {
    String m = "https://developer.mozilla.org";
    URL b = new URL(m);
    assertThat(b.href())
      .isEqualTo("https://developer.mozilla.org/");
  }

  @Test
  public void testMDN() throws URISyntaxException {
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
  public void testMerge() {
    assertThat(new URL("http://www.example.com", new URL("https://developer.mozilla.org")).href())
      .isEqualTo("http://www.example.com/");
  }

  @Test
  public void testMergeEmptyWithQueryContext() {
    assertThat(new URL("", "https://example.com/?query=1").href())
      .isEqualTo("https://example.com/?query=1"); // (Edge before 79 removes query arguments)
  }

  @Test
  public void testMergeRelativeDomain() {
    assertThat(new URL("//foo.com", "https://example.com").href())
      .isEqualTo("https://foo.com/");              // (see relative URLs)
  }

  @Test
  public void testQuotes() {
    assertThat(new URL("#/definitions/foo%22bar", "https://github.com/eclipse-vertx").href())
      .isEqualTo("https://github.com/eclipse-vertx#/definitions/foo%22bar");              // (see relative URLs)
  }

  @Test
  public void testRelativePathContext() {
    assertThat(new URL("folderIntegration.json", "http://localhost:1234/baseUriChange/").href())
      .isEqualTo("http://localhost:1234/baseUriChange/folderIntegration.json");              // (see relative URLs)
  }

  @Test
  public void testUrnURL() {
    assertThat(new URL("urn:vertxschemas:2ea1a0cf-b474-43d0-8167-c2babeb52990").href())
      .isEqualTo("urn:vertxschemas:2ea1a0cf-b474-43d0-8167-c2babeb52990");
  }
}
