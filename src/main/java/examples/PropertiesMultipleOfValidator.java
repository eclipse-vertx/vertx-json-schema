package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.BaseSyncValidator;
import io.vertx.json.schema.common.ValidatorContext;

import java.util.Map;

import static io.vertx.json.schema.common.JsonUtil.unwrap;

public class PropertiesMultipleOfValidator extends BaseSyncValidator {

  private int multipleOf;

  public PropertiesMultipleOfValidator(int multipleOf) {
    this.multipleOf = multipleOf;
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
    final Object orig = in;
    in = unwrap(in);
    if (in instanceof Map<?, ?>) { // If it's not an object, we skip the validation
      Map<?, ?> obj = (Map<?, ?>) in;
      if (obj.size() % multipleOf != 0) {
        throw ValidationException
          .create(
            "The provided object size is not a multiple of " + multipleOf,
            "propertiesMultipleOf",
            orig
          );
      }
    }
  }
}
