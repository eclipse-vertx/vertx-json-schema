/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.common;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.ValidationException;

import java.net.URI;

public class RefSchema extends SchemaImpl {

  private static final Logger log = LoggerFactory.getLogger(RefSchema.class);

  private final JsonPointer refPointer;
  private final SchemaParser schemaParser;
  private SchemaInternal cachedSchema;

  private final boolean executeSchemaValidators;

  public RefSchema(JsonObject schema, JsonPointer scope, SchemaParser schemaParser, MutableStateValidator parent, boolean executeSchemaValidators) {
    super(schema, scope, parent);
    this.schemaParser = schemaParser;
    this.executeSchemaValidators = executeSchemaValidators;
    try {
      String unparsedUri = schema.getString("$ref");
      refPointer = URIUtils.createJsonPointerFromURI(URI.create(unparsedUri));
      if (log.isDebugEnabled()) log.debug(String.format("Parsed %s ref for schema %s", refPointer, schema));
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null $ref keyword", e);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for $ref keyword", e);
    } catch (IllegalArgumentException e) {
      throw new SchemaException(schema, "$ref URI is invalid: " + e.getMessage(), e);
    }
  }

  private void registerCachedSchema(SchemaInternal s) {
    this.cachedSchema = s;
    if (s instanceof SchemaImpl)
      ((SchemaImpl) s).registerReferredSchema(this);
  }

  @Override
  public Future<Void> validateAsync(ValidatorContext inContext, Object in) {
    if (isSync()) return validateSyncAsAsync(inContext, in);
    ValidatorContext context = generateValidationContext(inContext);
    if (cachedSchema == null) {
      Future<Void> fut = schemaParser
        .getSchemaRouter()
        .resolveRef(refPointer, this.getScope(), schemaParser)
        .compose(
          s -> {
            if (s == null)
              return Future.failedFuture(ValidationException.createException("Cannot resolve reference " + this.refPointer.toURI(), "$ref", in));
            SchemaInternal solvedSchema = (SchemaInternal) s;
            registerCachedSchema(solvedSchema);
            if (log.isDebugEnabled()) log.debug(String.format("Solved ref %s as %s", refPointer, s.getScope()));
            if (solvedSchema instanceof RefSchema) {
              // We need to call solved schema validateAsync to solve upper ref, then we can update sync status
              return solvedSchema.validateAsync(context, in).compose(v -> {
                this.triggerUpdateIsSync();
                return Future.succeededFuture();
              });
            } else {
              this.triggerUpdateIsSync();
              return solvedSchema.validateAsync(context, in);
            }
          },
          err -> Future.failedFuture(ValidationException.createException("Error while resolving reference " + this.refPointer.toURI(), "$ref", in, err))
        );
      if (executeSchemaValidators) {
        return fut.compose(v -> this.runAsyncValidators(context, in));
      }
      return fut;
    } else {
      if (executeSchemaValidators) {
        return cachedSchema
          .validateAsync(context, in)
          .compose(v -> this.runAsyncValidators(context, in));
      } else {
        return cachedSchema.validateAsync(context, in);
      }
    }
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException {
    this.checkSync();
    context = generateValidationContext(context);
    // validateSync in RefSchema asserts that a cached schema exists
    cachedSchema.validateSync(context, in);
    if (executeSchemaValidators) {
      runSyncValidator(context, in);
    }
  }

  @Override
  public boolean calculateIsSync() {
    return (!executeSchemaValidators || super.calculateIsSync()) && cachedSchema != null && cachedSchema.isSync();
  }

  @Override
  protected void initializeIsSync() {
    isSync = false;
  }

  @Override
  public Object getDefaultValue() {
    this.checkSync();
    return cachedSchema.getDefaultValue();
  }

  @Override
  public boolean hasDefaultValue() {
    this.checkSync();
    return cachedSchema.hasDefaultValue();
  }

  @Override
  public void doApplyDefaultValues(Object obj) {
    this.checkSync();
    ((SchemaImpl) cachedSchema).doApplyDefaultValues(obj);
  }

  synchronized Future<SchemaInternal> trySolveSchema() {
    if (cachedSchema == null) {
      return schemaParser
        .getSchemaRouter()
        .resolveRef(refPointer, this.getScope(), schemaParser)
        .compose(
          s -> {
            if (s == null)
              return Future.failedFuture(ValidationException.createException("Cannot resolve reference " + this.refPointer.toURI(), "$ref", null));
            registerCachedSchema((SchemaInternal) s);
            if (log.isDebugEnabled()) log.debug(String.format("Solved ref %s as %s", refPointer, s.getScope()));
            if (s instanceof RefSchema) {
              // We need to call solved schema validateAsync to solve upper ref, then we can update sync status
              return ((RefSchema) s).trySolveSchema().map(s1 -> {
                this.triggerUpdateIsSync();
                return cachedSchema;
              });
            } else {
              this.triggerUpdateIsSync();
              return Future.succeededFuture(cachedSchema);
            }
          },
          err -> Future.failedFuture(ValidationException.createException("Error while resolving reference " + this.refPointer.toURI(), "$ref", null, err))
        );
    } else return Future.succeededFuture(cachedSchema);
  }

  void prePropagateSyncState() {
    isSync = true;
    if (getParent() != null)
      getParent().triggerUpdateIsSync();
  }

  void setIsSync(boolean s) {
    isSync = s;
    if (getParent() != null)
      getParent().triggerUpdateIsSync();
  }
}
