package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.Schema;

public abstract class BaseSingleSchemaValidator extends BaseMutableStateValidator {

  protected Schema schema;

  public BaseSingleSchemaValidator(MutableStateValidator parent) {
    super(parent);
  }

  @Override
  public boolean calculateIsSync() {
    return schema.isSync();
  }

  void setSchema(Schema schema) {
    this.schema = schema;
    this.initializeIsSync();
  }

}
