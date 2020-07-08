package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class ItemsValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new ItemsValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "items";
  }

  class ItemsValidator extends BaseSingleSchemaValidator implements DefaultApplier {

    public ItemsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          schema.validateSync(context.lowerLevelContext(), arr.getValue(i));
        }
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        List<Future> futs = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          Future<Void> f = schema.validateAsync(context.lowerLevelContext(), arr.getValue(i));
          if (f.isComplete()) {
            if (f.failed()) return Future.failedFuture(f.cause());
          } else {
            futs.add(f);
          }
        }
        if (futs.isEmpty())
          return Future.succeededFuture();
        else
          return CompositeFuture.all(futs).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public void applyDefaultValue(Object value) {
      if (value instanceof JsonArray) {
        ((JsonArray)value).forEach(((SchemaImpl)schema)::doApplyDefaultValues);
      }
    }
  }
}
