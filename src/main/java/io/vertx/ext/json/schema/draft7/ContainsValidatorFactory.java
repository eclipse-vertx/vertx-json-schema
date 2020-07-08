package io.vertx.ext.json.schema.draft7;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.BaseSingleSchemaValidator;
import io.vertx.ext.json.schema.common.BaseSingleSchemaValidatorFactory;
import io.vertx.ext.json.schema.common.MutableStateValidator;
import io.vertx.ext.json.schema.common.ValidatorContext;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class ContainsValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new ContainsValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "contains";
  }

  class ContainsValidator extends BaseSingleSchemaValidator {

    public ContainsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        if (arr.isEmpty())
          return Future.failedFuture(createException("provided array should not be empty", "contains", in));
        else
          return CompositeFuture.any(
            arr
              .stream()
              .map(i -> schema.validateAsync(context.lowerLevelContext(), in))
              .collect(Collectors.toList())
          ).compose(
            cf -> {
              IntStream.rangeClosed(0, cf.size())
                .forEach(i -> {
                  if (cf.succeeded(i)) {
                    context.markEvaluatedItem(i);
                  }
                });
              return Future.succeededFuture();
            },
            err -> Future.failedFuture(createException("provided array doesn't contain an element matching the contains schema", "contains", in, err))
          );
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      ValidationException t = null;
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          try {
            schema.validateSync(context.lowerLevelContext(), arr.getValue(i));
            context.markEvaluatedItem(i);
            return;
          } catch (ValidationException e) {
            t = e;
          }
        }
        throw createException("provided array doesn't contain an element matching the contains schema", "contains", in, t);
      }
    }

  }

}
