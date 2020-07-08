package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaParser;

import java.net.URI;

public interface SchemaParserInternal extends SchemaParser {

  @Override
  default SchemaInternal parse(JsonObject jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parse(Boolean jsonSchema) {
    return parse(jsonSchema, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parseFromString(String unparsedJson) {
    return parseFromString(unparsedJson, new SchemaURNId().toPointer());
  }

  @Override
  default SchemaInternal parse(JsonObject jsonSchema, JsonPointer schemaPointer) {
    return parse((Object) jsonSchema, schemaPointer);
  }

  @Override
  default SchemaInternal parse(Boolean jsonSchema, JsonPointer schemaPointer) {
    return parse((Object) jsonSchema, schemaPointer);
  }

  SchemaInternal parse(Object jsonSchema, JsonPointer scope, MutableStateValidator parent);

  default SchemaInternal parse(Object jsonSchema, JsonPointer scope) {
    return parse(jsonSchema, scope, null);
  }

  default SchemaInternal parse(Object jsonSchema, URI scope, MutableStateValidator parent) {
    return this.parse(jsonSchema, JsonPointer.fromURI(scope), parent);
  }

  default SchemaInternal parse(Object jsonSchema, URI scope) {
    return parse(jsonSchema, scope, null);
  }

  SchemaInternal parseFromString(String unparsedJson, JsonPointer scope, MutableStateValidator parent);

  default SchemaInternal parseFromString(String unparsedJson, JsonPointer schemaPointer) {
    return parseFromString(unparsedJson, schemaPointer, null);
  }

  ;

  default SchemaInternal parseFromString(String unparsedJson, URI scope, MutableStateValidator parent) {
    return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), parent);
  }

  default SchemaInternal parseFromString(String unparsedJson, URI scope) {
    return this.parseFromString(unparsedJson, JsonPointer.fromURI(scope), null);
  }


}
