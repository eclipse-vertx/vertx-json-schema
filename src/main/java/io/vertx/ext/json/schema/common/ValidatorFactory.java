package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaException;

/**
 * Factory for {@link Validator}. This is the entrypoint if you want to create a new custom keyword
 */
public interface ValidatorFactory {
  /**
   * This method consume the schema eventually creating a new {@link Validator}. The schema parser calls it during schema parsing only if {@link #canConsumeSchema(JsonObject)} returns true <br/>
   *
   * You can return any of {@link SyncValidator}, {@link AsyncValidator} or {@link MutableStateValidator}
   *
   * @param schema JsonObject representing the schema
   * @param scope scope of the parsed schema
   * @param parser caller parser
   * @param parent parent of this schema
   * @throws SchemaException if the keyword(s) handled by this ValidatorFactory are invalid
   * @return the created validator.
   */
  Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) throws SchemaException;

  /**
   * Returns true if this factory can consume the provided schema, eventually returning an instance of {@link Validator}
   * @param schema
   * @return
   */
  boolean canConsumeSchema(JsonObject schema);
}
