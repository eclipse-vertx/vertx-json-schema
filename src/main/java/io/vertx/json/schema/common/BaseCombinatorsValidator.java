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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.json.schema.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.json.schema.common.JsonUtil.isArray;
import static io.vertx.json.schema.common.JsonUtil.isObject;

public abstract class BaseCombinatorsValidator extends BaseMutableStateValidator implements DefaultApplier {

  protected SchemaInternal[] schemas;

  public BaseCombinatorsValidator(MutableStateValidator parent) {
    super(parent);
  }

  @Override
  public boolean calculateIsSync() {
    return Arrays.stream(schemas).map(Schema::isSync).reduce(true, Boolean::logicalAnd);
  }

  void setSchemas(List<SchemaInternal> schemas) {
    this.schemas = schemas.toArray(new SchemaInternal[0]);
    Arrays.sort(this.schemas, ValidatorPriority.COMPARATOR);
    this.initializeIsSync();
  }

  @Override
  public Future<Void> applyDefaultValue(Object obj) {
    if (!(isObject(obj) || isArray(obj))) {
      return Future.succeededFuture();
    }

    if (this.isSync()) {
      for (Schema s : schemas) {
        ((SchemaImpl) s).getOrApplyDefaultAsync(obj);
      }
      return Future.succeededFuture();
    }
    return CompositeFuture.all(
      Arrays.stream(schemas)
        .map(s -> s.getOrApplyDefaultAsync(obj))
        .collect(Collectors.toList())
    ).mapEmpty();
  }
}
