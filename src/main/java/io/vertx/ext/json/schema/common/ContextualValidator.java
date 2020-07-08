package io.vertx.ext.json.schema.common;

public abstract class ContextualValidator extends BaseSingleSchemaValidator {
  public ContextualValidator(MutableStateValidator parent) {
    super(parent);
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.CONTEXTUAL_VALIDATOR;
  }
}
