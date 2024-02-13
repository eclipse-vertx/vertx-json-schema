package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import io.vertx.json.schema.impl.DefaultJsonFormatValidatorImpl;

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
    JsonSchemaOptions jsonSchemaOptionsWithFormatValidator = new JsonSchemaOptions()
      .setBaseUri("https://vertx.io")
      //Specify your own format validator here!
      .setJsonFormatValidator(new DefaultJsonFormatValidatorImpl());

    SchemaRepository repository =
      SchemaRepository.create(jsonSchemaOptionsWithFormatValidator);
  }
}
