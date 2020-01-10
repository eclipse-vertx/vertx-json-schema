package io.vertx.ext.json.schema.draft7.dsl;

public enum StringFormat {
  BYTE("byte"),
  DATE("date"),
  DATETIME("date-time"),
  IPV4("ipv4"),
  IPV6("ipv6"),
  HOSTNAME("hostname"),
  EMAIL("email"),
  URI("uri"),
  URI_REFERENCE("uri-reference"),
  REGEX("regex"),
  JSON_POINTER("json-pointer"),
  RELATIVE_JSON_POINTER("relative-json-pointer"),
  URI_TEMPLATE("uti-template"),
  TIME("time");

  private String name;

  StringFormat(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
