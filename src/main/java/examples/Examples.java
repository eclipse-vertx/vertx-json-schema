package examples;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.docgen.Source;
import io.vertx.ext.json.schema.*;

@Source
public class Examples {

  public void instantiate(Vertx vertx) {
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser draft7SchemaParser = SchemaParser.createDraft7SchemaParser(schemaRouter);
  }

  public void parse(SchemaParser parser, JsonObject object, JsonPointer schemaPointer) {
    Schema schema = parser.parse(object, schemaPointer);
  }

  public void parseNoId(SchemaParser parser, JsonObject object) {
    Schema schema = parser.parse(object);
    schema.getScope(); // Get generated scope of schema (schema pointer)
  }

  public void validateSync(Schema schema, Object json) {
    try {
      schema.validateSync(json);
      // Successful validation
    } catch (ValidationException e) {
      // Failed validation
    } catch (NoSyncValidationException e) {
      // Cannot validate synchronously. You must validate using validateAsync
    }
  }

  public void validateAsync(Schema schema, Object json) {
    schema.validateAsync(json).setHandler(ar -> {
      if (ar.succeeded()) {
        // Validation succeeded
      } else {
        // Validation failed
        ar.cause(); // Contains ValidationException
      }
    });
  }

  public void applyDefaultValues(Schema schema, JsonObject jsonObject, JsonArray jsonArray) {
    schema.applyDefaultValues(jsonObject);
    // Or
    schema.applyDefaultValues(jsonArray);
  }

  public void customFormat(SchemaParser parser) {
    parser.withStringFormatValidator("firstUppercase", str -> Character.isUpperCase(str.charAt(0)));

    JsonObject mySchema = new JsonObject().put("format", "firstUppercase");
    Schema schema = parser.parse(mySchema);
  }

  public void mountSyncKeyword(SchemaParser parser) {
    parser.withValidatorFactory(new PropertiesMultipleOfValidatorFactory());

    JsonObject mySchema = new JsonObject().put("propertiesMultipleOf", 2);
    Schema schema = parser.parse(mySchema);
  }

  public void mountAsyncKeyword(SchemaParser parser, Vertx vertx) {
    parser.withValidatorFactory(new AsyncEnumValidatorFactory(vertx));

    JsonObject mySchema = new JsonObject().put("asyncEnum", "enums.myapplication");
    Schema schema = parser.parse(mySchema);
  }

}
