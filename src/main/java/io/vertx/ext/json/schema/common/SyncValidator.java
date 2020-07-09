package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

public interface SyncValidator extends Validator {

  /**
   * Validate the provided value
   *
   * @param context
   * @param in
   * @throws ValidationException       if the object is not valid
   * @throws NoSyncValidationException if no sync validation can be provided
   */
  void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException;

}
