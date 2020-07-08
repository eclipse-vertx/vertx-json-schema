package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class AllOfValidatorFactory extends BaseCombinatorsValidatorFactory {

  @Override
  BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new AllOfValidator(parent);
  }

  @Override
  String getKeyword() {
    return "allOf";
  }

  class AllOfValidator extends BaseCombinatorsValidator {

    public AllOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      for (Schema s : schemas) s.validateSync(in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return CompositeFuture
        .all(Arrays.stream(schemas).map(s -> s.validateAsync(in)).collect(Collectors.toList()))
        .compose(res -> Future.succeededFuture(), Future::failedFuture);
    }
  }

}
