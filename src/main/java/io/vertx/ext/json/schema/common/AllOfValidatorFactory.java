package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

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
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      for (Schema s : schemas) s.validateSync(in);
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      return FutureUtils.andThen(
          CompositeFuture.all(Arrays.stream(schemas).map(s -> s.validateAsync(in)).collect(Collectors.toList())),
          res -> Future.succeededFuture(),
        Future::failedFuture);
    }
  }

}
