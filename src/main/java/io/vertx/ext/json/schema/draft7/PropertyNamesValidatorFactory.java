package io.vertx.ext.json.schema.draft7;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.common.MutableStateValidator;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.BaseSingleSchemaValidator;
import io.vertx.ext.json.schema.common.BaseSingleSchemaValidatorFactory;
import io.vertx.ext.json.schema.common.FutureUtils;

import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class PropertyNamesValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new PropertyNamesValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "propertyNames";
  }

  class PropertyNamesValidator extends BaseSingleSchemaValidator {

    public PropertyNamesValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonObject) {
        ((JsonObject) in).getMap().keySet().stream().forEach(schema::validateSync);
      }
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      if (in instanceof JsonObject){
        return FutureUtils.andThen(
            CompositeFuture.all(
              ((JsonObject) in).getMap().keySet().stream().map(schema::validateAsync).collect(Collectors.toList())
            ),
            cf -> Future.succeededFuture(),
            err -> Future.failedFuture(createException("provided object contains a key not matching the propertyNames schema", "propertyNames", in, err))
        );
      } else return Future.succeededFuture();
    }
  }

}
