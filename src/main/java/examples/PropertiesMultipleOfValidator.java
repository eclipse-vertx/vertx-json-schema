package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.BaseSyncValidator;
import io.vertx.json.schema.common.ValidatorContext;

public class PropertiesMultipleOfValidator extends BaseSyncValidator {

  private final int multipleOf;

  public PropertiesMultipleOfValidator(int multipleOf) {
    this.multipleOf = multipleOf;
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
    if (in instanceof JsonObject) { // If it's not an object, we skip the validation
      if (((JsonObject) in).size() % multipleOf != 0) {
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
