package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class AnyOfValidatorFactory extends BaseCombinatorsValidatorFactory {

  @Override
  BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new AnyOfValidator(parent);
  }

  @Override
  String getKeyword() {
    return "anyOf";
  }

  class AnyOfValidator extends BaseCombinatorsValidator {

    public AnyOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      ValidationException res = null;
      for (SchemaInternal s : this.schemas) {
        try {
          s.validateSync(context, in);
          return;
        } catch (ValidationException e) {
          res = e;
        }
      }
      throw res;
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return CompositeFuture
        .any(Arrays.stream(this.schemas).map(s -> s.validateAsync(in)).collect(Collectors.toList()))
        .compose(
          res -> Future.succeededFuture(),
          err -> Future.failedFuture(createException("anyOf subschemas don't match", "anyOf", in, err))
        );
    }

  }

}
