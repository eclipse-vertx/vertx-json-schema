package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.HashSet;
import java.util.Set;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class RequiredValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator validator) {
    try {
      JsonArray keys = (JsonArray) schema.getValue("required");
      return new RequiredValidator(new HashSet(keys.getList()));
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for enum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null enum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("required");
  }

  public class RequiredValidator extends BaseSyncValidator {
    private final Set<String> requiredKeys;

    public RequiredValidator(Set<String> requiredKeys) {
      this.requiredKeys = requiredKeys;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        for (String k : requiredKeys) {
          if (!obj.containsKey(k)) throw createException("provided object should contain property " + k, "required", in);
        }
      }
    }
  }

}
