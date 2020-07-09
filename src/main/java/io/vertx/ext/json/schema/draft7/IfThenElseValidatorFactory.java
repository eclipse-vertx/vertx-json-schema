package io.vertx.ext.json.schema.draft7;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.ValidationException;
import io.vertx.ext.json.schema.common.*;

import java.util.Map;

public class IfThenElseValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      IfThenElseValidator validator = new IfThenElseValidator(parent);
      Object conditionSchemaUnparsed = schema.getValue("if");
      SchemaInternal conditionSchema = parser.parse((conditionSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) conditionSchemaUnparsed) : conditionSchemaUnparsed, scope.copy().append("if"), validator);
      Object thenSchemaUnparsed = schema.getValue("then");
      SchemaInternal thenSchema = (thenSchemaUnparsed == null) ? null : parser.parse((thenSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) thenSchemaUnparsed) : thenSchemaUnparsed, scope.copy().append("if"), validator);
      Object elseSchemaUnparsed = schema.getValue("else");
      SchemaInternal elseSchema = (elseSchemaUnparsed == null) ? null : parser.parse((elseSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) elseSchemaUnparsed) : elseSchemaUnparsed, scope.copy().append("if"), validator);
      validator.configure(conditionSchema, thenSchema, elseSchema);
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for if/then/else keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null if/then/else keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("if") && (schema.containsKey("then") || schema.containsKey("else"));
  }

  class IfThenElseValidator extends BaseMutableStateValidator {

    private SchemaInternal condition;
    private SchemaInternal thenBranch;
    private SchemaInternal elseBranch;

    public IfThenElseValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(final SchemaInternal condition, final SchemaInternal thenBranch, final SchemaInternal elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
      this.initializeIsSync();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      boolean conditionResult;
      try {
        condition.validateSync(context, in);
        conditionResult = true;
      } catch (ValidationException e) {
        conditionResult = false;
      }

      if (conditionResult) {
        if (thenBranch != null) this.thenBranch.validateSync(context, in);
      } else {
        if (elseBranch != null) this.elseBranch.validateSync(context, in);
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return this.condition.validateAsync(context, in).compose(
        o -> (this.thenBranch != null) ? this.thenBranch.validateAsync(context, in) : Future.succeededFuture(),
        o -> (this.elseBranch != null) ? this.elseBranch.validateAsync(context, in) : Future.succeededFuture()
      );
    }

    @Override
    public boolean calculateIsSync() {
      return condition.isSync() && (thenBranch == null || thenBranch.isSync()) && (elseBranch == null || elseBranch.isSync());
    }
  }

}
