package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.Schema;

/**
 * Schema should implement sync and async validator too
 */
public interface SchemaInternal extends Schema, AsyncValidator, SyncValidator {
}
