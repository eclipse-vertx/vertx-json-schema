package examples;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.common.MutableStateValidator;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.Validator;
import io.vertx.json.schema.common.ValidatorFactory;

public class AsyncEnumValidatorFactory implements ValidatorFactory {

  public final static String KEYWORD_NAME = "asyncEnum";

  private Vertx vertx;

  public AsyncEnumValidatorFactory(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String address = (String) schema.getValue(KEYWORD_NAME);
      return new AsyncEnumValidator(vertx, address);
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
