package examples;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.common.BaseAsyncValidator;

import static io.vertx.ext.json.schema.ValidationException.createException;

class AsyncEnumValidator extends BaseAsyncValidator {

  private Vertx vertx;
  private String address;

  public AsyncEnumValidator(Vertx vertx, String address) {
    this.vertx = vertx;
    this.address = address;
  }

  @Override
  public Future<Void> validateAsync(Object in) {
    Promise<Void> promise = Promise.promise();
    // Retrieve the valid values from the event bus
    vertx.eventBus().request(address, new JsonObject(), ar -> {
      JsonArray enumValues = (JsonArray) ar.result().body();
      if (!enumValues.contains(in))
        promise.fail(createException("Not matching async enum " + enumValues, "asyncEnum", in));
      else
        promise.complete();
    });
    return promise.future();
  }
}
