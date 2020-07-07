package io.vertx.ext.json.schema.common;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.SchemaRouter;

import java.net.URI;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public abstract class BaseSchemaParser implements SchemaParserInternal {

  protected final List<ValidatorFactory> validatorFactories;
  protected final SchemaRouter router;

  protected BaseSchemaParser(SchemaRouter router) {
    this.router = router;
    this.validatorFactories = initValidatorFactories();
  }

  @Override
  public SchemaRouter getSchemaRouter() {
    return router;
  }

  @Override
  public Schema parse(Object jsonSchema, final JsonPointer scope, MutableStateValidator parent) {
    if (jsonSchema instanceof Map) jsonSchema = new JsonObject((Map<String, Object>) jsonSchema);
    if (jsonSchema instanceof JsonObject) {
      JsonObject json = (JsonObject) jsonSchema;
      Set<Validator> validators = new HashSet<>();

      // 1. No $id, No $anchor, just add the schema with the provided scope
      // 2. $id build the schema using $id (eventually resolved) and addSchema(schema), then addSchema(schema, scope)
      // 2.1 If $id has also a fragment, add it as alias ( <= draft-7)
      // 2.2 If $anchor, add it as alias ( >= draft2019-09)

      Map.Entry<Optional<JsonPointer>, Optional<String>> e = resolveIdAndAlias(json, scope.getURIWithoutFragment());

      SchemaImpl s = e
        .getKey()
        .map(id -> createSchema(json, id, parent)).orElseGet(() -> createSchema(json, scope, parent));

      if (e.getKey().isPresent()) {
        router.addSchema(s, scope);
      } else {
        router.addSchema(s);
      }

      e.getValue().ifPresent(alias -> router.addSchemaAlias(s, alias));

      for (ValidatorFactory factory : validatorFactories) {
        if (factory.canConsumeSchema(json)) {
          Validator v = factory.createValidator(json, e.getKey().orElse(scope).copy(), this, s);
          if (v != null) validators.add(v);
        }
      }
      s.setValidators(validators);
      return s;
    } else if (jsonSchema instanceof Boolean) {
      Schema s = ((Boolean) jsonSchema) ? TrueSchema.getInstance() : FalseSchema.getInstance();
      router.addSchema(s);
      return s;
    } else
      throw new SchemaException(jsonSchema, "Schema must be a JsonObject or a Boolean");
  }

  protected SchemaImpl createSchema(JsonObject schema, JsonPointer scope, MutableStateValidator parent) {
    if (schema.containsKey("$ref")) return new RefSchema(schema, scope, this, parent);
    else return new SchemaImpl(schema, scope, parent);
  }

  protected abstract List<ValidatorFactory> initValidatorFactories();

  protected Map.Entry<Optional<JsonPointer>, Optional<String>> resolveIdAndAlias(JsonObject schema, URI scope) {
    // 2.1 If $id has also a fragment, add it as alias ( <= draft-7)
    Optional<JsonPointer> id = Optional.empty();
    Optional<String> alias = Optional.empty();

    // Resolve the scope looking in $id
    if (schema.containsKey("$id")) {
      URI originalId = URI.create(schema.getString("$id"));
      URI idWithoutFragment = URIUtils.removeFragment(originalId);
      if (originalId.isAbsolute()) {
        id = Optional.of(JsonPointer.fromURI(idWithoutFragment));
      } else if (originalId.getPath() != null && !originalId.getPath().isEmpty()) {
        id = Optional.of(JsonPointer.fromURI(URIUtils.resolvePath(scope, idWithoutFragment.getPath())));
      }

      if (originalId.getFragment() != null && !originalId.getFragment().isEmpty()) {
        alias = Optional.of(originalId.getFragment());
      }
    }

    return new SimpleImmutableEntry<>(id, alias);
  }

  @Override
  public BaseSchemaParser withValidatorFactory(ValidatorFactory factory) {
    this.validatorFactories.add(factory);
    return this;
  }

  @Override
  public BaseSchemaParser withStringFormatValidator(String formatName, Predicate<String> predicate) {
    BaseFormatValidatorFactory f = (BaseFormatValidatorFactory) validatorFactories
        .stream()
        .filter(factory -> factory instanceof BaseFormatValidatorFactory)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("This json schema version doesn't support format keyword"));
    f.addStringFormatValidator(formatName, predicate);
    return this;
  }

  @Override
  public Schema parseFromString(String unparsedJson, JsonPointer scope, MutableStateValidator parent) {
    return this.parse(Json.decodeValue(unparsedJson.trim()), scope, parent);
  }
}
