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
package io.vertx.ext.json.schema.common.dsl;

import java.util.function.Supplier;

public class Keyword {

  private String keyword;
  private Supplier<Object> value;

  public Keyword(String keyword, Supplier<Object> value) {
    this.keyword = keyword;
    this.value = value;
  }

  public Keyword(String keyword, Object value) {
    this.keyword = keyword;
    this.value = () -> value;
  }

  public String getKeyword() {
    return keyword;
  }

  public Supplier<Object> getValueSupplier() {
    return value;
  }
}
