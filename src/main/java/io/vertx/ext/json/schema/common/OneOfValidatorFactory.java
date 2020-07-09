package io.vertx.ext.json.schema.common;

import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class OneOfValidatorFactory extends BaseCombinatorsValidatorFactory {

  @Override
  BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new OneOfValidator(parent);
  }

  @Override
  String getKeyword() {
    return "oneOf";
  }

  class OneOfValidator extends BaseCombinatorsValidator {

    public OneOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    private boolean isValidSync(SchemaInternal schema, ValidatorContext context, Object in) {
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
      long validCount = Arrays.stream(schemas).map(s -> isValidSync(s, context, in)).filter(b -> b.equals(true)).count();
      if (validCount > 1) throw createException("More than one schema valid", "oneOf", in);
      else if (validCount == 0) throw createException("No schema matches", "oneOf", in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return FutureUtils
        .oneOf(Arrays.stream(schemas).map(s -> s.validateAsync(context, in)).collect(Collectors.toList()))
        .recover(err -> Future.failedFuture(createException("No schema matches", "oneOf", in, err)));
    }
  }

}
