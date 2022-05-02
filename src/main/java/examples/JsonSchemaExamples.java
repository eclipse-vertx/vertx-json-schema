package examples;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;

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
}
