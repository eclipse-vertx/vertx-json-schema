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
package io.vertx.json.schema.common.dsl;

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

  private final String name;

  StringFormat(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
