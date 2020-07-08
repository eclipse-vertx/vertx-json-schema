package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.Schema;

import java.util.*;
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

  public Map<String, RouterNode> getChilds() {
    return childs;
  }

  public Stream<RouterNode> flattened() {
    return flattenedList().stream();
  }

  public Stream<RouterNode> reverseFlattened() {
    List<RouterNode> output = flattenedList();
    Collections.reverse(output);
    return output.stream();
  }

  private List<RouterNode> flattenedList() {
    Stack<RouterNode> nodesToScan = new Stack<>();
    nodesToScan.push(this);
    List<RouterNode> output = new ArrayList<>();

    while (!nodesToScan.isEmpty()) {
      RouterNode next = nodesToScan.pop();
      if (!output.contains(next)) {
        next.childs.values().forEach(nodesToScan::push);
        output.add(next);
      }
    }

    return output;
  }
}
