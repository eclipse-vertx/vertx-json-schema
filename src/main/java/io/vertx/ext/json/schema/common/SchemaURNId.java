package io.vertx.ext.json.schema.common;

import io.vertx.core.json.pointer.JsonPointer;

import java.net.URI;
import java.util.UUID;

public class SchemaURNId {

  String id;

  public SchemaURNId(String id) {
    if (!id.matches("[a-zA-Z0-9_-]*")) throw new IllegalArgumentException("Id must match pattern [a-zA-Z0-9_-]*");
    this.id = id;
  }

  public SchemaURNId() {
    this.id = UUID.randomUUID().toString();
  }

  public JsonPointer toPointer() {
    return JsonPointer.fromURI(toURI());
  }

  public URI toURI() {
    return URI.create(toString());
  }

  @Override
  public String toString() {
    return "urn:vertxschemas:" + id;
  }
}
