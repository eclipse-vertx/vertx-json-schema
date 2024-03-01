package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import io.vertx.json.schema.common.dsl.Schemas;

public class JsonSchemaExamples {

  public void instantiate() {
    SchemaRepository repository =
      SchemaRepository.create(new JsonSchemaOptions().setBaseUri("https://vertx.io"));
  }

  public void parse(JsonObject object, SchemaRepository repository) {
    JsonSchema schema = JsonSchema.of(object);

    // Or

    repository.dereference(JsonSchema.of(object));
  }

  public void validate(JsonSchema schema, Object json) {
    OutputUnit result = Validator.create(
        schema,
        new JsonSchemaOptions().setDraft(Draft.DRAFT7))
      .validate(json);

    if (result.getValid()) {
      // Successful validation
    }
  }

  public void instantiateWithCustomJsonFormatValidator() {
    JsonFormatValidator customFormatValidator = (instanceType, format, instance) -> {
      if ("string".equals(instanceType) && "allUpercase".equals(format)) {
        if (instance.toString().equals(instance.toString().toUpperCase())) {
          return null;
        }
        return String.format("String does not match the format \"%s\"", format);
      }
      return null;
    };

    SchemaRepository repository = SchemaRepository.create(new JsonSchemaOptions(), customFormatValidator);

    JsonSchema schema = JsonSchema.of(Schemas.stringSchema().toJson());
    Validator validator = Validator.create(schema, new JsonSchemaOptions(), customFormatValidator);
  }
}
