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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;

import java.util.Arrays;
import java.util.List;

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
    this.schemas = schemas.toArray(new SchemaInternal[schemas.size()]);
    Arrays.sort(this.schemas, ValidatorPriority.COMPARATOR);
    this.initializeIsSync();
  }

  @Override
  public void applyDefaultValue(Object obj) {
    if (!(obj instanceof JsonObject || obj instanceof JsonArray)) return;
    for (Schema s : schemas) {
      ((SchemaImpl) s).doApplyDefaultValues(obj);
    }
  }
}
