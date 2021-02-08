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
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.ValidationException;

import java.net.URI;

import static io.vertx.json.schema.ValidationException.createException;

public class RecursiveRefSchema extends SchemaImpl {

  private static final Logger log = LoggerFactory.getLogger(RecursiveRefSchema.class);

  private final JsonPointer refPointer;
  private final SchemaParser schemaParser;

  public RecursiveRefSchema(JsonObject schema, JsonPointer scope, SchemaParser schemaParser, MutableStateValidator parent) {
    super(schema, scope, parent);
    this.schemaParser = schemaParser;
    try {
      String unparsedUri = schema.getString("$recursiveRef");
      refPointer = URIUtils.createJsonPointerFromURI(URI.create(unparsedUri));
      if (log.isDebugEnabled()) log.debug(String.format("Parsed %s $recursiveRef for schema %s", refPointer, schema));
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null $recursiveRef keyword", e);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for $recursiveRef keyword", e);
    } catch (IllegalArgumentException e) {
      throw new SchemaException(schema, "$recursiveRef URI is invalid: " + e.getMessage(), e);
    }
  }

  @Override
  public Future<Void> validateAsync(ValidatorContext inContext, Object in) {
    // A recursive ref is always cached!!!
    SchemaInternal solvedSchema;
    try {
      solvedSchema = resolveSchema(inContext);
    } catch (SchemaException e) {
      return Future.failedFuture(createException("Error while resolving $recursiveRef " + refPointer.toURI(), "$recursiveRef", in, e));
    }
    if (solvedSchema == null) {
      return Future.failedFuture(createException("Cannot resolve $recursiveRef " + refPointer.toURI(), "$recursiveRef", in));
    }

    ValidatorContext newContext = generateValidationContext(solvedSchema, inContext);

    if (solvedSchema.isSync() && this.isSync) {
      try {
        solvedSchema.validateSync(RecursiveAnchorValidatorContextDecorator.unwrap(newContext));
        runSyncValidator(newContext, in);
      } catch (ValidationException e) {
        return Future.failedFuture(e);
      }
    }
    return solvedSchema
      .validateAsync(RecursiveAnchorValidatorContextDecorator.unwrap(newContext), in)
      .compose(v -> runAsyncValidators(newContext, in));
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException {
    this.checkSync();
  }

  @Override
  public boolean isSync() {
    return false;
  }

  @Override
  protected void checkSync() throws ValidationException, NoSyncValidationException {
    throw new NoSyncValidationException("Trying to execute validateSync() for a $recursiveRef schema", this);
  }

  private SchemaInternal resolveSchema(ValidatorContext inContext) {
    return (SchemaInternal) schemaParser.getSchemaRouter().resolveCachedSchema(this.refPointer, computeScope(inContext), schemaParser);
  }

  private JsonPointer computeScope(ValidatorContext context) {
    if (context instanceof RecursiveAnchorValidatorContextDecorator) {
      RecursiveAnchorValidatorContextDecorator decorator = (RecursiveAnchorValidatorContextDecorator) context;
      return decorator.getRecursiveAnchor();
    }
    return this.getScope();
  }

  protected ValidatorContext generateValidationContext(SchemaInternal schema, ValidatorContext parent) {
    ValidatorContext context = (schema instanceof SchemaImpl && ((SchemaImpl) schema).shouldRecordContext) ? parent.startRecording() : parent;
    if (this.recursiveAnchor) {
      return RecursiveAnchorValidatorContextDecorator.wrap(context, this.getScope());
    }
    return context;
  }

  @Override
  public Future<Object> getOrApplyDefaultAsync(Object input) {
    return Future.succeededFuture(input); // TODO Does it really makes sense default on $recursiveRef?
  }

  @Override
  public Object getOrApplyDefaultSync(Object input) {
    return input; // TODO Does it really makes sense default on $recursiveRef?
  }

}
