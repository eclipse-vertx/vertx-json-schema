package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.Schema;

import java.util.Arrays;
import java.util.List;

public abstract class BaseCombinatorsValidator extends BaseMutableStateValidator implements ValidatorWithDefaultApply {

  protected Schema[] schemas;

  public BaseCombinatorsValidator(MutableStateValidator parent) {
    super(parent);
  }

  @Override
  public boolean calculateIsSync() {
    return Arrays.stream(schemas).map(Schema::isSync).reduce(true, Boolean::logicalAnd);
  }

  void setSchemas(List<Schema> schemas) {
    this.schemas = schemas.toArray(new Schema[schemas.size()]);
    this.initializeIsSync();
  }

  @Override
  public void applyDefaultValue(Object obj) {
    if (!(obj instanceof JsonObject || obj instanceof JsonArray)) return;
    for (Schema s : schemas) {
      ((SchemaImpl)s).doApplyDefaultValues(obj);
    }
  }
}
