package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

/**
 * Interface representing a <a href="https://json-schema.org/">Json Schema</a> <br/>
 *
 * A schema could have two states: <br/>
 * <ul>
 *   <li>Synchronous: The validators tree can provide a synchronous validation, so you can validate your json both using {@link this#validateSync(Object)} and {@link this#validateAsync(Object)}</li>
 *   <li>Asynchronous: One or more branches of the validator tree requires an asynchronous validation, so you must use {@link this#validateAsync(Object)} to validate your json. If you use {@link this#validateSync(Object)} it will throw a {@link NoSyncValidationException}</li>
 * </ul>
 *
 * To check the schema state you can use method {@link this#isSync()} <br/>
 * The schema can mutate the state in time, e.g. if you have a schema that is asynchronous because of a {@code $ref},
 * after the first validation the external schema is cached inside {@link SchemaRouter} and this schema will switch to synchronous state<br/>
 *
 */
@VertxGen
public interface Schema {

  /**
   * Validate the json performing an asynchronous validation. Returns a failed future with {@link ValidationException} if json doesn't match the schema.<br/>
   *
   * Note: If the schema is synchronous, this method will call internally {@link this#validateSync(Object)}
   *
   * @param json
   * @return
   */
  Future<Void> validateAsync(Object json);

  /**
   * Validate the json performing a synchronous validation. Throws a {@link ValidationException} if json doesn't match the schema.<br/>
   *
   * @param json
   * @throws ValidationException
   * @throws NoSyncValidationException If the schema cannot perform a synchronous validation
   */
  void validateSync(Object json) throws ValidationException, NoSyncValidationException;

  /**
   * Get scope of this schema
   *
   * @return
   */
  JsonPointer getScope();

  /**
   * Get Json representation of the schema
   *
   * @return
   */
  Object getJson();

  /**
   * Return the default value defined in the schema
   *
   * @return
   */
  Object getDefaultValue();

  /**
   * Return true if the schema has a default value defined
   *
   * @return
   */
  boolean hasDefaultValue();

  /**
   * This function mutates {@code array} applying default values, when available.
   *
   * @param array
   * @throws NoSyncValidationException if this schema represents a {@code $ref} not solved yet
   */
  void applyDefaultValues(JsonArray array) throws NoSyncValidationException;

  /**
   * This function mutates {@code object} applying default values, when available.
   *
   * @param object
   * @throws NoSyncValidationException if this schema represents a {@code $ref} not solved yet
   */
  void applyDefaultValues(JsonObject object) throws NoSyncValidationException;

  /**
   * Returns true if this validator can actually provide a synchronous validation
   *
   * @return
   */
  boolean isSync();

}
