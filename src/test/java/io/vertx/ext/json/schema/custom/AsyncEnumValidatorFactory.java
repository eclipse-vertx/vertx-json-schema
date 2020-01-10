package io.vertx.ext.json.schema.custom;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class AsyncEnumValidatorFactory implements ValidatorFactory {

  Vertx vertx;

  public AsyncEnumValidatorFactory(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String address = (String) schema.getValue("asyncEnum");
      return new AsyncEnumValidator(vertx, address);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for propertiesMultipleOf keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null propertiesMultipleOf keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("asyncEnum");
  }

  private class AsyncEnumValidator extends BaseAsyncValidator {

    Vertx vertx;
    String address;

    public AsyncEnumValidator(Vertx vertx, String address) {
      this.vertx = vertx;
      this.address = address;
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      Promise<Void> fut = Promise.promise();
      vertx.eventBus().request(address, new JsonObject(), ar -> {
        JsonArray enumValues = (JsonArray) ar.result().body();
        if (!enumValues.contains(in)) fut.fail(createException("Not matching async enum", "asyncEnum", in));
        else fut.complete();
      });
      return fut.future();
    }
  }

}
