package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class RouterNode {
  private Schema schema;
  private final Map<String, RouterNode> childs;

  public RouterNode() {
    this(null);
  }

  public RouterNode(Schema schema) {
    this.schema = schema;
    this.childs = new HashMap<>();
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public Schema getSchema() {
    return schema;
  }

  public boolean hasSchema() {
    return schema != null;
  }

  public Map<String, RouterNode> getChilds() {
    return childs;
  }

  public Stream<RouterNode> flattened() {
    return Stream.concat(
        (schema == null) ? Stream.empty() : Stream.of(this),
        childs.values().stream().flatMap(RouterNode::flattened)
    );
  }

  public Stream<RouterNode> reverseFlattened() {
    return Stream.concat(
        childs.values().stream().flatMap(RouterNode::reverseFlattened),
        (schema == null) ? Stream.empty() : Stream.of(this)
    );
  }
}
