package io.vertx.ext.json.schema.draft7;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

import java.util.Map;

public class IfThenElseValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      IfThenElseValidator validator = new IfThenElseValidator(parent);
      Object conditionSchemaUnparsed = schema.getValue("if");
      Schema conditionSchema = parser.parse((conditionSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) conditionSchemaUnparsed) : conditionSchemaUnparsed, scope.copy().append("if"), validator);
      Object thenSchemaUnparsed = schema.getValue("then");
      Schema thenSchema = (thenSchemaUnparsed == null) ? null : parser.parse((thenSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) thenSchemaUnparsed) : thenSchemaUnparsed, scope.copy().append("if"), validator);
      Object elseSchemaUnparsed = schema.getValue("else");
      Schema elseSchema = (elseSchemaUnparsed == null) ? null : parser.parse((elseSchemaUnparsed instanceof Map) ? new JsonObject((Map<String, Object>) elseSchemaUnparsed) : elseSchemaUnparsed, scope.copy().append("if"), validator);
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

    private Schema condition;
    private Schema thenBranch;
    private Schema elseBranch;

    public IfThenElseValidator(MutableStateValidator parent) {
      super(parent);
    }

    private void configure(final Schema condition, final Schema thenBranch, final Schema elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
      this.initializeIsSync();
    }

    @Override
    public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      boolean conditionResult;
      try {
        condition.validateSync(in);
        conditionResult = true;
      } catch (ValidationException e) {
        conditionResult = false;
      }

      if (conditionResult) {
        if (thenBranch != null) this.thenBranch.validateSync(in);
      } else {
        if (elseBranch != null) this.elseBranch.validateSync(in);
      }
    }

    @Override
    public Future<Void> validateAsync(Object in) {
      if (isSync()) return validateSyncAsAsync(in);
      return FutureUtils.andThen(
          this.condition.validateAsync(in),
          o -> (this.thenBranch != null) ? this.thenBranch.validateAsync(in): Future.succeededFuture(),
          o -> (this.elseBranch != null) ? this.elseBranch.validateAsync(in) : Future.succeededFuture()
      );
    }

    @Override
    public boolean calculateIsSync() {
      return condition.isSync() && (thenBranch == null || thenBranch.isSync()) && (elseBranch == null || elseBranch.isSync());
    }
  }

}
