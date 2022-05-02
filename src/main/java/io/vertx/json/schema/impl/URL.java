package io.vertx.json.schema.impl;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a simple URL split/merge functionality similar to MDN URL which behavior is required for resolving
 * references in json-schema.org
 */
public class URL {

  private static final Pattern URL_PATTERN = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

  //  protocol: "https"
  private String scheme;
  //  hostname: "www.example.com:8080"
  private String authority;
  private String path;
  //  query: "?fr=yset_ie_syc_oracle&type=orcl_hpset"
  private String query;
  //  hash: "page0"
  private String fragment;

  public URL(String url) {
    this(url, true);
  }

  private URL(String url, boolean strict) {
    if (strict) {
      if (url == null) {
        throw new NullPointerException("Url isn't valid: null");
      }
      Matcher matcher = URL_PATTERN.matcher(escape(url));
      if (matcher.matches()) {
        scheme = matcher.group(2);
        authority = matcher.group(4);
        path = matcher.group(5);
        query = matcher.group(7);
        fragment = matcher.group(9);
      } else {
        throw new IllegalArgumentException("Url isn't valid: " + url);
      }

      if (scheme == null) {
        throw new IllegalStateException("(strict) url isn't valid: " + url);
      }

      if (authority == null) {
        switch (scheme) {
          case "file":
          case "urn":
            // OK
            break;
          default:
            throw new IllegalStateException("(strict) url isn't valid: " + url);
        }
      }

      if (authority != null) {
        // normalize
        if (path != null && !path.startsWith("/")) {
          path = "/" + path;
        }
      }

      } else {
      if (url != null && url.length() > 0) {
        Matcher matcher = URL_PATTERN.matcher(escape(url));
        if (matcher.matches()) {
          scheme = matcher.group(2);
          authority = matcher.group(4);
          path = matcher.group(5);
          query = matcher.group(7);
          fragment = matcher.group(9);
        }
      }
    }

    if (authority == null) {
      // relative url
      if ("".equals(path)) {
        // normalize
        if (query != null || fragment != null) {
          path = null;
        }
      }
    }
  }

  public URL(String url, String base) {
    this(url, base == null || base.length() == 0 ? null : new URL(base, true));
  }

  public URL(String url, URL base) {
    URL uri = new URL(url, base == null);
    // set the context
    if (base != null) {
      this.scheme = base.scheme;
      this.authority = base.authority;
      this.path = base.path;
      this.query = base.query;
      this.fragment = base.fragment;
    }
    // start merging
    if (uri.scheme != null) {
      this.scheme = uri.scheme;
      // reset lower
      this.authority = null;
      this.path = null;
      this.query = null;
      this.fragment = null;
    }
    if (uri.authority != null) {
      this.authority = uri.authority;
      // reset lower (authority isn't null, default to "/")
      this.path = "/";
      this.query = null;
      this.fragment = null;
    }
    if (uri.path != null) {
      if (uri.path.startsWith("/")) {
        this.path = uri.path;
      } else {
        // relative path requires a path merge if current path is already set
        if (this.path != null) {
          int sep = this.path.lastIndexOf('/');
          if (sep != -1) {
            this.path = this.path.substring(0, sep + 1) + uri.path;
          } else {
            // no path set yet
            if (this.authority != null) {
              this.path = "/" + uri.path;
            } else {
              this.path = uri.path;
            }
          }
        } else {
          this.path = uri.path;
        }
      }
      // reset lower
      this.query = null;
      this.fragment = null;
    }
    if (uri.query != null) {
      this.query = uri.query;
      // reset lower
      this.fragment = null;
    }
    if (uri.fragment != null) {
      this.fragment = uri.fragment;
    }
  }

  public String scheme() {
    if (scheme == null || scheme.length() == 0) {
      return "";
    }
    return scheme + ":";
  }

  public String authority() {
    if (authority == null || authority.length() == 0) {
      return "";
    }
    return authority;
  }

  public String path() {
    if (path == null || path.length() == 0) {
      return "";
    }

    return path;
  }

  public String query() {
    if (query == null || query.length() == 0) {
      return "";
    }
    return "?" + query;
  }

  public String fragment() {
    if (fragment == null || fragment.length() == 0) {
      return "";
    }
    return "#" + fragment;
  }

  public URL anchor(String fragment) {
    if (fragment != null) {
      if (fragment.length() == 0) {
        fragment = null;
      } else {
        // https://url.spec.whatwg.org/#dom-url-hash
        if (fragment.startsWith("#")) {
          fragment = fragment.substring(1);
        }
        if (fragment.length() > 0) {
          fragment = escape(fragment);
        }
      }
    }
    this.fragment = fragment;
    return this;
  }

  public String href() {
    return
      // scheme
      (scheme == null || scheme.length() == 0 ? "" : scheme + ":") +
        // authority
        (authority == null ? "" : "//" + authority) +
        // path
        (path == null ? "" : path) +
        // query
        (query == null ? "" : "?" + query) +
        // fragment
        (fragment == null ? "" : "#" + fragment);
  }

  private static final String genDelims = ":?#@/"; // except []
  private static final String subDelims = "!$&'()*+,;=";
  private static final String unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
  private static final String okChars = genDelims + subDelims + unreserved + "%"; // don't double-escape %-escaped chars!

  private static String escape(String p) {

    StringBuilder encoded = new StringBuilder(p.length());

    byte[] bytes = p.getBytes(StandardCharsets.UTF_8);
    for (byte aByte : bytes) {
      if (okChars.indexOf(aByte) >= 0) {
        encoded.append((char) aByte);
      } else {
        // encode
        encoded
          .append('%')
          .append(Integer.toHexString(Byte.toUnsignedInt(aByte)).toUpperCase(Locale.ROOT));
      }
    }

    return encoded.toString();
  }

  @Override
  public String toString() {
    return href();
  }
}
