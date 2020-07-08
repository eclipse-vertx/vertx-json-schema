package io.vertx.ext.json.schema.common;

import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class NotValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new NotValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "not";
  }

  class NotValidator extends BaseSingleSchemaValidator {

    public NotValidator(MutableStateValidator parent) {
      super(parent);
    }

    private boolean isValidSync(ValidatorContext context, Object in) {
      try {
        schema.validateSync(context, in);
        return true;
      } catch (ValidationException e) {
        return false;
      }
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (isValidSync(context, in)) throw createException("input should be invalid", "not", in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return schema
        .validateAsync(context, in)
        .compose(
          res -> Future.failedFuture(createException("input should be invalid", "not", in)),
          err -> Future.succeededFuture()
        );
    }

  }

}
