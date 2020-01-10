package io.vertx.ext.json.schema.common;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.json.schema.*;

import java.util.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class SchemaImpl extends BaseMutableStateValidator implements Schema {

  private static final Logger log = LoggerFactory.getLogger(SchemaImpl.class);

  private final JsonObject schema;
  private final JsonPointer scope;
  private Validator[] validators;

  private final Set<RefSchema> referringSchemas;

  SchemaImpl(JsonObject schema, JsonPointer scope, MutableStateValidator parent) {
    super(parent);
    this.schema = schema;
    this.scope = scope;
    referringSchemas = new HashSet<>();
  }

  @Override
  public JsonPointer getScope() {
    return scope;
  }

  @Override
  public JsonObject getJson() {
    return schema;
  }

  @Override
  public Object getDefaultValue() {
    return schema.getValue("default");
  }

  @Override
  public boolean hasDefaultValue() {
    return schema.containsKey("default");
  }

  @Override
  public void applyDefaultValues(JsonArray array) throws NoSyncValidationException {
    doApplyDefaultValues(array);
  }

  @Override
  public void applyDefaultValues(JsonObject object) throws NoSyncValidationException {
    doApplyDefaultValues(object);
  }


  public void doApplyDefaultValues(Object obj) {
    for (Validator v : validators) {
      if (v instanceof ValidatorWithDefaultApply) {
        ((ValidatorWithDefaultApply) v).applyDefaultValue(obj);
      }
    }
  }

  @Override
  public void triggerUpdateIsSync() {
    boolean calculated = calculateIsSync();
    boolean previous = isSync;
    isSync = calculated;
    if (calculated != previous) {
      if (!referringSchemas.isEmpty())
        referringSchemas.forEach(r -> r.setIsSync(calculated));
      if (getParent() != null)
        getParent().triggerUpdateIsSync();
    }
  }

  @Override
  public Future<Void> validateAsync(Object in) {
    if (isSync()) return validateSyncAsAsync(in);
    if (log.isDebugEnabled()) log.trace(String.format("Starting async validation for schema %s and input %s", schema, in));

    List<Future> futures = new ArrayList<>();
    for (Validator validator : validators) {
      if (!validator.isSync()) {
        Future<Void> asyncValidate = ((AsyncValidator)validator).validateAsync(in);
        asyncValidate = asyncValidate.recover(t -> fillException(t, in));
        futures.add(asyncValidate);
      } else try {
        ((SyncValidator)validator).validateSync(in);
      } catch (ValidationException e) {
        e.setSchema(this);
        e.setScope(this.scope);
        return Future.failedFuture(e);
      }
    }
    if (!futures.isEmpty()) {
      return CompositeFuture.all(futures).compose(cf -> Future.succeededFuture());
    } else {
      return Future.succeededFuture();
    }
  }

  @Override
  public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
    this.checkSync();
    for (Validator validator : validators) {
      try {
        ((SyncValidator)validator).validateSync(in);
      } catch (ValidationException e) {
        e.setSchema(this);
        e.setScope(this.scope);
        throw e;
      }
    }
  }

  @Override
  public boolean calculateIsSync() {
    return validators.length == 0 || Arrays.stream(validators).map(Validator::isSync).reduce(true, Boolean::logicalAnd);
  }

  void setValidators(Set<Validator> validators) {
    this.validators = validators.toArray(new Validator[0]);
    Arrays.sort(this.validators, ValidatorPriority.VALIDATOR_COMPARATOR);
    this.initializeIsSync();
  }

  private Future<Void> fillException(Throwable e, Object in) {
    if (e instanceof ValidationException) {
      ValidationException ve = (ValidationException) e;
      ve.setSchema(this);
      ve.setScope(this.scope);
      return Future.failedFuture(ve);
    } else {
      return Future.failedFuture(createException("Error while validating", null, in, e));
    }
  }

  void registerReferredSchema(RefSchema ref) {
      referringSchemas.add(ref);
      if (log.isDebugEnabled()) {
        log.trace(String.format("Ref schema %s reefers to schema %s",  ref, this));
        log.trace(String.format("Ref schemas that refeers to %s: %s", this, this.referringSchemas.size()));
      }
      // This is a trick to solve the circular references.
      // 1. for each ref that reefers to this schema we propagate isSync = true to the upper levels.
      //    If this schema isSync = false only because its childs contains refs to itself, after the pre propagation
      //    this schema isSync = true, otherwise is still false
      // 2. for each ref schema we set the isSync calculated and propagate to upper levels of refs
      referringSchemas.forEach(RefSchema::prePropagateSyncState);
      referringSchemas.forEach(r -> r.setIsSync(this.isSync));

  }
}
