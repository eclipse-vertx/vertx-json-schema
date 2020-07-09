package io.vertx.ext.json.schema.common;

public abstract class BaseSingleSchemaValidator extends BaseMutableStateValidator {

  protected SchemaInternal schema;

  public BaseSingleSchemaValidator(MutableStateValidator parent) {
    super(parent);
  }

  @Override
  public boolean calculateIsSync() {
    return schema.isSync();
  }

  public void setSchema(SchemaInternal schema) {
    this.schema = schema;
    this.initializeIsSync();
  }

}
