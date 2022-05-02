package examples;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.*;

public class JsonSchemaExamples {

  public void instantiate(Vertx vertx) {
    SchemaRepository repository =
      SchemaRepository.create(new JsonSchemaOptions().setBaseUri("https://vertx.io"));
  }

  public void parse(SchemaParser parser, JsonObject object, JsonPointer schemaPointer) {
    JsonSchema schema = JsonSchema.of(object);
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
