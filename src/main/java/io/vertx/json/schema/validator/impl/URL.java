package io.vertx.json.schema.validator.impl;

import java.net.URI;
import java.net.URISyntaxException;

public class URL {

  //  protocol: "https:"
  private String protocol = null;
  //  hostname: "www.example.com"
  private String hostname = null;
  //  port: "8080"
  private int port = -1;
  //  pathname: "/"
  private String pathname = null;
  //  search: "?fr=yset_ie_syc_oracle&type=orcl_hpset"
  private String search = null;
  //  hash: "page0"
  private String hash = null;

  public URL(String url) {
    this(url, true);
  }

  private URL(String url, boolean strict) {
    try {
      URI uri = new URI(url);

      if (uri.getScheme() != null) {
        setProtocol(uri.getScheme());
      } else {
        if (strict) {
          throw new RuntimeException(url + " is not a valid URL");
        }
      }
      if (uri.getHost() != null) {
        setHostname(uri.getHost());
      } else {
        if (strict) {
          throw new RuntimeException(url + " is not a valid URL");
        }
      }
      if (uri.getPort() != -1) {
        setPort(uri.getPort());
      }
      if (uri.getPath() != null) {
        if (strict) {
          if (uri.getPath().length() == 0) {
            setPathname("/");
          } else {
            setPathname(uri.getPath());
          }
        } else {
          setPathname(uri.getPath());
        }
      }
      if (uri.getQuery() != null) {
        setSearch(uri.getQuery());
      }
      if (uri.getFragment() != null) {
        setHash(uri.getFragment(), strict);
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public URL(String url, String base) {
    this(url, base == null || base.length() == 0 ? null : new URL(base, false));
  }

  public URL(String url, URL base) {
    URL uri = new URL(url, base == null);

    if (base != null) {
      setProtocol(base.protocol);
      setHostname(base.hostname);
      setPort(base.port);
      setPathname(base.pathname);
      setSearch(base.search);
      setHash(base.hash);
    }

    if (uri.protocol != null) {
      setProtocol(uri.protocol);
    }
    if (uri.hostname != null) {
      setHostname(uri.hostname);
    }
    if (uri.port != -1) {
      setPort(uri.port);
    }
    if (uri.pathname != null) {
      setPathname(uri.pathname);
    }
    if (uri.search != null) {
      setSearch(uri.search);
    }
    if (uri.hash != null) {
      setHash(uri.hash);
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
    if (search == null) {
      return "";
    }
    return "?" + search;
  }

  public URL setSearch(String search) {
    if (search != null) {
      if (search.startsWith("?")) {
        search = search.substring(1);
      }
      if (search.length() == 0) {
        search = null;
      }
    }
    this.search = search;
    this.hash = null;
    return this;
  }

  public String getHash() {
    if (hash == null) {
      return "";
    }
    return "#" + hash;
  }

  public URL setHash(String hash) {
    return setHash(hash, true);
  }
  private URL setHash(String hash, boolean strict) {
    if (hash != null && strict) {
      // https://url.spec.whatwg.org/#dom-url-hash
      if (hash.startsWith("#")) {
        hash = hash.substring(1);
      }
      if (hash.length() == 0) {
        hash = null;
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
      getProtocol() + "//" + getHost() + getPathname() + getSearch() + getHash();
  }

  @Override
  public String toString() {
    return href();
  }
}
