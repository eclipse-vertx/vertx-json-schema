package io.vertx.json.schema.validator.impl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class URL {

  //  protocol: "https:"
  private String protocol;
  //  hostname: "www.example.com"
  private String userInfo;
  private String hostname;
  //  port: "8080"
  private int port = -1;
  //  pathname: "/"
  private String pathname;
  //  search: "?fr=yset_ie_syc_oracle&type=orcl_hpset"
  private String search;
  //  hash: "page0"
  private String hash;

  public URL(String url) {
    this(url, true);
  }

  private URL(String url, boolean strict) {
    try {
      if (strict) {
        java.net.URL _url = new java.net.URL(url);

        this.protocol = _url.getProtocol();
        if (this.protocol == null) {
          throw new RuntimeException(url + " is not a valid URL");
        }
        this.hostname = _url.getHost();
        if (this.hostname == null) {
          if (!"file".equals(this.protocol)) {
            throw new RuntimeException(url + " is not a valid URL");
          }
        }
        this.userInfo = _url.getUserInfo();
        this.port = _url.getPort();
        this.pathname = _url.getPath();
        this.search = _url.getQuery();
        this.hash = _url.getRef();
      } else {
        if (url != null && url.length() > 0) {
          java.net.URI _url = new java.net.URI(url);

          this.protocol = _url.getScheme();
          this.hostname = _url.getHost();
          this.userInfo = _url.getUserInfo();
          this.port = _url.getPort();
          this.pathname = _url.getPath();
          // no path, should be handled as a "null" if no protocol
          if ("".equals(this.pathname)) {
            if (this.protocol == null) {
              this.pathname = null;
            }
          }
          if (this.pathname != null && this.pathname.length() > 0 && this.pathname.charAt(0) != '/') {
            this.pathname = "/" + pathname;
          }
          this.search = _url.getQuery();
          this.hash = _url.getFragment();
        }
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public URL(String url, String base) {
    this(url, base == null || base.length() == 0 ? null : new URL(base, true));
  }

  public URL(String url, URL base) {
    URL uri = new URL(url, base == null);

    if (base != null) {
      this.protocol = base.protocol;
      this.hostname = base.hostname;
      this.port = base.port;
      this.pathname = base.pathname;
      this.search = base.search;
      this.hash = base.hash;
    }

    if (uri.protocol != null) {
      this.protocol = uri.protocol;
      this.hostname = null;
      this.port = -1;
      this.pathname = null;
      this.search = null;
      this.hash = null;
    }
    if (uri.hostname != null) {
      this.hostname = uri.hostname;
      this.port = -1;
      this.pathname = null;
      this.search = null;
      this.hash = null;
    }
    if (uri.port != -1) {
      this.port = uri.port;
      this.pathname = null;
      this.search = null;
      this.hash = null;
    }
    if (uri.pathname != null) {
      this.pathname = uri.pathname;
      this.search = null;
      this.hash = null;
    }
    if (uri.search != null) {
      this.search = uri.search;
      this.hash = null;
    }
    if (uri.hash != null) {
      this.hash = uri.hash;
    }
  }

  public String getProtocol() {
    if (protocol == null) {
      return "";
    }
    return protocol + ":";
  }

  public URL setProtocol(String protocol) {
    if (protocol != null) {
      if (protocol.length() == 0) {
        protocol = null;
      }
    }
    this.protocol = protocol;
    return this;
  }

  public String getUserInfo() {
    if (userInfo == null) {
      return "";
    }
    return userInfo;
  }

  public URL setUserInfo(String userInfo) {
    if (userInfo != null) {
      if (userInfo.length() == 0) {
        userInfo = null;
      }
    }
    this.userInfo = userInfo;
    return this;
  }

  public String getHostname() {
    if (hostname == null) {
      return "";
    }
    return hostname;
  }

  public URL setHostname(String hostname) {
    if (hostname != null) {
      if (hostname.length() == 0) {
        hostname = null;
      }
    }
    this.hostname = hostname;
    return this;
  }

  public int getPort() {
    return port;
  }

  public URL setPort(int port) {
    this.port = port;
    return this;
  }

  public String getPathname() {
    if (pathname == null) {
      return "";
    }
    if (pathname.length() == 0) {
      return "/";
    }
    return pathname;
  }
  private URL setPathname(String pathname) {
    if (pathname != null) {
      if (pathname.length() == 0) {
        pathname = null;
      } else {
        if (!pathname.startsWith("/")) {
          pathname = "/" + pathname;
        }
      }
    }
    this.pathname = pathname;
    this.search = null;
    this.hash = null;
    return this;
  }

  public String getSearch() {
    if (search == null || search.length() == 0) {
      return "";
    }
    return "?" + search;
  }

  public URL setSearch(String search) {
    if (search != null) {
      if (search.length() == 0) {
        search = null;
      } else {
        if (search.startsWith("?")) {
          search = search.substring(1);
        }
      }
    }
    this.search = search;
    this.hash = null;
    return this;
  }

  public String getHash() {
    if (hash == null || hash.length() == 0) {
      return "";
    }
    return "#" + hash;
  }

  public URL setHash(String hash) {
    if (hash != null) {
      if (hash.length() == 0) {
        hash = null;
      } else {
        // https://url.spec.whatwg.org/#dom-url-hash
        if (hash.startsWith("#")) {
          hash = hash.substring(1);
        }
      }
    }
    this.hash = hash;
    return this;
  }

  // synthetic values

  //  host: "www.example.com:8080"
  public String getHost() {
    if (getPort() != -1) {
      return getHostname() + ":" + getPort();
    } else {
      return getHostname();
    }
  }

  public String href() {
    return
        // protocol
        (protocol == null || protocol.length() == 0 ? "" : protocol + ":") +
          "//" +
          // userinfo
          (userInfo == null ? "" : userInfo) +
          // host
          (hostname == null ? "" : hostname) +
          // port
          (port == -1 ? "" : (":" + port)) +
          // path
          (pathname == null ? "" : pathname.length() == 0 ? "/" : encode(pathname)) +
          // search
          (search == null ? "" : "?" + encode(search)) +
          // hash
          (hash == null ? "" : "#" + encode(hash));
  }

  private static final String genDelims = ":?#@/"; // except []
  private static final String subDelims = "!$&'()*+,;=";
  private static final String unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
  private static final String okChars = genDelims + subDelims + unreserved + "%"; // don't double-escape %-escaped chars!

  private static String encode(String p) {

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
