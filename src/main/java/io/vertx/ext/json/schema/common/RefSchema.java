package io.vertx.ext.json.schema.common;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.net.URI;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class RefSchema extends SchemaImpl {

  private static final Logger log = LoggerFactory.getLogger(RefSchema.class);

  private final JsonPointer refPointer;
  private final SchemaParser schemaParser;
  private Schema cachedSchema;

  RefSchema(JsonObject schema, JsonPointer scope, SchemaParser schemaParser, MutableStateValidator parent) {
    super(schema, scope, parent);
    this.schemaParser = schemaParser;
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

  private void registerCachedSchema(Schema s) {
    this.cachedSchema = s;
    if (s instanceof SchemaImpl)
      ((SchemaImpl)s).registerReferredSchema(this);
  }

  @Override
  public Future<Void> validateAsync(Object in) {
    if (isSync()) return validateSyncAsAsync(in);
    if (cachedSchema == null) {
      return FutureUtils.andThen(
          schemaParser.getSchemaRouter().resolveRef(refPointer, this.getScope(), schemaParser),
          s -> {
            if (s == null) return Future.failedFuture(createException("Cannot resolve reference " + this.refPointer.toURI(), "$ref", in));
            registerCachedSchema(s);
            if (log.isDebugEnabled()) log.debug(String.format("Solved ref %s as %s", refPointer, s.getScope()));
            if (s instanceof RefSchema) {
              // We need to call solved schema validateAsync to solve upper ref, then we can update sync status
              return s.validateAsync(in).compose(v -> {
                  this.triggerUpdateIsSync();
                  return Future.succeededFuture();
              });
            } else {
              this.triggerUpdateIsSync();
              return s.validateAsync(in);
            }
          },
          err -> Future.failedFuture(createException("Error while resolving reference " + this.refPointer.toURI(), "$ref", in, err))
          );
    } else {
      return cachedSchema.validateAsync(in);
    }
  }

  @Override
  public void validateSync(Object in) throws ValidationException {
    this.checkSync();
    // validateSync in RefSchema asserts that a cached schema exists
    cachedSchema.validateSync(in);
  }

  @Override
  public boolean calculateIsSync() {
    return cachedSchema != null && cachedSchema.isSync();
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
    ((SchemaImpl)cachedSchema).doApplyDefaultValues(obj);
  }

  synchronized Future<Schema> trySolveSchema() {
    if (cachedSchema == null) {
      return FutureUtils.andThen(
          schemaParser.getSchemaRouter().resolveRef(refPointer, this.getScope(), schemaParser),
          s -> {
            if (s == null) return Future.failedFuture(createException("Cannot resolve reference " + this.refPointer.toURI(), "$ref", null));
            registerCachedSchema(s);
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
          err -> Future.failedFuture(createException("Error while resolving reference " + this.refPointer.toURI(), "$ref", null, err))
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
