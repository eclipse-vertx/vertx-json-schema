package io.vertx.ext.json.schema.openapi3;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class NullableValidatorFactory implements ValidatorFactory {

  private final static BaseSyncValidator NULL_VALIDATOR = new BaseSyncValidator() {
    @Override
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      if (in == null) throw createException("input cannot be null", "nullable", in);
    }
  };

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Boolean nullable = (Boolean) schema.getValue("nullable");
      if (nullable == null || !nullable) return NULL_VALIDATOR;
      else return null;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for nullable keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null nullable keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return !schema.containsKey("$ref");
  }

}
