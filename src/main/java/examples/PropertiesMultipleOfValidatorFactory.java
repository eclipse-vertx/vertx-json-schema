package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.common.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class PropertiesMultipleOfValidatorFactory implements ValidatorFactory {

  public final static String KEYWORD_NAME = "propertiesMultipleOf";

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number multipleOf = (Number) schema.getValue(KEYWORD_NAME);
      return new PropertiesMultipleOfValidator(multipleOf.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for " + KEYWORD_NAME + " keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null " + KEYWORD_NAME + " keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey(KEYWORD_NAME);
  }

}
