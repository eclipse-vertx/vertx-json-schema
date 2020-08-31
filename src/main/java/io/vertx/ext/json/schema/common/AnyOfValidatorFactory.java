package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
      List<Throwable> res = null;
      for (SchemaInternal s : this.schemas) {
        try {
          s.validateSync(context, in);
          return;
        } catch (ValidationException e) {
          if (res == null) {
             res = new ArrayList<>();
          }
          res.add(e);
        }
      }
      throw createException(
        "anyOf subschemas don't match",
        "anyOf",
        in,
        res
      );
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return CompositeFuture
        .any(Arrays.stream(this.schemas).map(s -> s.validateAsync(context, in)).collect(Collectors.toList()))
        .compose(cf -> {
          if (cf.succeeded()) {
            return Future.succeededFuture();
          } else {
            return Future.failedFuture(
              createException(
                "anyOf subschemas don't match",
                "anyOf",
                in,
                IntStream.range(0, cf.size()).mapToObj(cf::cause).filter(Objects::nonNull).collect(Collectors.toList()))
            );
          }
        });
    }

  }

}
