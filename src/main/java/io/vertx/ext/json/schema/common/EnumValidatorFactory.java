package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class EnumValidatorFactory implements ValidatorFactory {

  @SuppressWarnings("unchecked")
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonArray allowedValues = (JsonArray) schema.getValue("enum");
      Set allowedValuesParsed = (Set) allowedValues
          .getList().stream()
          .map(o ->
              (o instanceof Map) ? new JsonObject((Map<String, Object>) o) :
                  (o instanceof List) ? new JsonArray((List) o) :
                      o
          ).collect(Collectors.toSet());
      return new EnumValidator(allowedValuesParsed);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for enum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null enum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("enum");
  }

  public class EnumValidator extends BaseSyncValidator {
    private final Object[] allowedValues;

    public EnumValidator(Set allowedValues) {
      this.allowedValues = allowedValues.toArray();
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.MAX_PRIORITY;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      for (int i = 0; i < allowedValues.length; i++) {
        if (ComparisonUtils.equalsNumberSafe(allowedValues[i], in))
          return;
      }
      throw createException("Input doesn't match one of allowed values of enum: " + allowedValues, "enum", in);
    }
  }

}
