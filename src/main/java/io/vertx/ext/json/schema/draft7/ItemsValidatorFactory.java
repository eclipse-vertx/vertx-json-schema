package io.vertx.ext.json.schema.draft7;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemsValidatorFactory extends io.vertx.ext.json.schema.common.ItemsValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    Object itemsSchema = schema.getValue("items");
    if (itemsSchema instanceof JsonArray) {
      try {
        JsonPointer baseScope = scope.copy().append("items");
        JsonArray itemsList = (JsonArray) itemsSchema;
        List<Schema> parsedSchemas = new ArrayList<>();

        ItemByItemValidator validator = new ItemByItemValidator(parent);
        for (int i = 0; i < itemsList.size(); i++) {
          parsedSchemas.add(i, parser.parse(itemsList.getValue(i), baseScope.copy().append(Integer.toString(i)), validator));
        }
        if (schema.containsKey("additionalItems"))
          validator.configure(parsedSchemas.toArray(new Schema[parsedSchemas.size()]), parser.parse(schema.getValue("additionalItems"), scope.copy().append("additionalItems"), validator));
        else
          validator.configure(parsedSchemas.toArray(new Schema[parsedSchemas.size()]), null);
        return validator;
      } catch (NullPointerException e) {
        throw new SchemaException(schema, "Null items keyword", e);
      }
    } else {
      return super.createValidator(schema, scope, parser, parent);
    }
  }

  class ItemByItemValidator extends BaseMutableStateValidator implements ValidatorWithDefaultApply {

    Schema[] schemas;
    Schema additionalItems;

    public ItemByItemValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(Schema[] schemas, Schema additionalItems) {
      this.schemas = schemas;
      this.additionalItems = additionalItems;
      initializeIsSync();
    }

    @Override
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          if (i >= schemas.length) {
            if (additionalItems != null)
              additionalItems.validateSync(arr.getValue(i));
          } else
            schemas[i].validateSync(arr.getValue(i));
        }
      }
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      if (in instanceof JsonArray) {
        List<Future> futures = new ArrayList<>();
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          Future<Void> fut;
          if (i >= schemas.length) {
            if (additionalItems != null)
              fut = additionalItems.validateAsync(arr.getValue(i));
            else continue;
          } else fut = schemas[i].validateAsync(arr.getValue(i));
          if (fut.isComplete()) {
            if (fut.failed()) return Future.failedFuture(fut.cause());
          } else {
            futures.add(fut);
          }
        }
        if (futures.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futures).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public boolean calculateIsSync() {
      return (additionalItems == null || additionalItems.isSync()) && Arrays.stream(schemas).map(Schema::isSync).reduce(true, Boolean::logicalAnd);
    }

    @Override
    public void applyDefaultValue(Object value) {
      if (value instanceof JsonArray) {
        JsonArray arr = (JsonArray) value;
        for (int i = 0; i < arr.size(); i++) {
          if (i >= schemas.length) {
            if (additionalItems != null)
              ((SchemaImpl)additionalItems).doApplyDefaultValues(arr.getValue(i));
          } else
            ((SchemaImpl)schemas[i]).doApplyDefaultValues(arr.getValue(i));
        }
      }
    }
  }

}
