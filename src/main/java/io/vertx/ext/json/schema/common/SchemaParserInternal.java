package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.Schema;
import io.vertx.ext.json.schema.SchemaParser;

import java.net.URI;

public interface SchemaParserInternal extends SchemaParser {

  @Override
  default Schema parse(JsonObject jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default Schema parse(Boolean jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default Schema parseFromString(String unparsedJson) {
    return parseFromString(unparsedJson, new SchemaURNId().toPointer());
  }

  @Override
  default Schema parse(JsonObject jsonSchema, JsonPointer schemaPointer) {
    return parse((Object)jsonSchema, schemaPointer);
  }

  @Override
  default Schema parse(Boolean jsonSchema, JsonPointer schemaPointer) {
    return parse((Object)jsonSchema, schemaPointer);
  }

  Schema parse(Object jsonSchema, JsonPointer scope, MutableStateValidator parent);

  default Schema parse(Object jsonSchema, JsonPointer scope) { return parse(jsonSchema, scope, null); }

  default Schema parse(Object jsonSchema, URI scope, MutableStateValidator parent) { return this.parse(jsonSchema, JsonPointer.fromURI(scope), parent); }

  default Schema parse(Object jsonSchema, URI scope) { return parse(jsonSchema, scope, null); }

  Schema parseFromString(String unparsedJson, JsonPointer scope, MutableStateValidator parent);

  default Schema parseFromString(String unparsedJson, JsonPointer schemaPointer) { return parseFromString(unparsedJson, schemaPointer,  null); };

  default Schema parseFromString(String unparsedJson, URI scope, MutableStateValidator parent) { return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), parent); }

  default Schema parseFromString(String unparsedJson, URI scope) { return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), null); }


}
