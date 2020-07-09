package io.vertx.ext.json.schema.draft201909;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DependentSchemasValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject dependencies = schema.getJsonObject("dependentSchemas");
      JsonPointer baseScope = scope.copy().append("dependentSchemas");

      DependentSchemasValidator validator = new DependentSchemasValidator(parent);
      validator.configure(
        dependencies.stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
          parser.parse(entry.getValue(), baseScope.copy().append(entry.getKey()), validator)
        ))
      );
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for dependentSchemas keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null dependentSchemas keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("dependentSchemas");
  }

  class DependentSchemasValidator extends BaseMutableStateValidator {

    Map<String, SchemaInternal> keySchemaDeps;

    public DependentSchemasValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(Map<String, SchemaInternal> keySchemaDeps) {
      this.keySchemaDeps = keySchemaDeps;
      initializeIsSync();
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        List<Future> futs = keySchemaDeps
          .entrySet()
          .stream()
          .filter(e -> obj.containsKey(e.getKey()))
          .map(e -> e.getValue().validateAsync(context, in))
          .collect(Collectors.toList());
        if (futs.isEmpty()) return Future.succeededFuture();
        else return CompositeFuture.all(futs).mapEmpty();
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        keySchemaDeps
          .entrySet()
          .stream()
          .filter(e -> obj.containsKey(e.getKey()))
          .forEach(e -> e.getValue().validateSync(context, in));
      }
    }

    @Override
    public boolean calculateIsSync() {
      return keySchemaDeps.values().stream().map(Schema::isSync).reduce(true, Boolean::logicalAnd);
    }
  }

}
