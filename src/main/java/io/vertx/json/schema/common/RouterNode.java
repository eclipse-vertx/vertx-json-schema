/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.common;

import io.vertx.json.schema.Schema;

import java.util.*;
import java.util.stream.Stream;

class RouterNode {
  private SchemaInternal schema;
  private final Map<String, RouterNode> childs;

  public RouterNode() {
    this(null);
  }

  public RouterNode(SchemaInternal schema) {
    this.schema = schema;
    this.childs = new HashMap<>();
  }

  public void setSchema(SchemaInternal schema) {
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
