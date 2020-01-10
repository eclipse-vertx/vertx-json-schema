package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.BaseSyncValidator;

public class PropertiesMultipleOfValidator extends BaseSyncValidator {

  private int multipleOf;

  public PropertiesMultipleOfValidator(int multipleOf) {
    this.multipleOf = multipleOf;
  }

  @Override
  public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
    if (in instanceof JsonObject) { // If it's not an object, we skip the validation
      if (((JsonObject)in).size() % multipleOf != 0) {
        throw ValidationException
          .createException(
            "The provided object size is not a multiple of " + multipleOf,
            "propertiesMultipleOf",
            in
          );
      }
    }
  }
}
