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
import io.vertx.json.schema.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

public class SchemaImpl extends BaseMutableStateValidator implements SchemaInternal {

  private static final Logger log = LoggerFactory.getLogger(SchemaImpl.class);

  private final JsonObject schema;
  private final JsonPointer scope;
  private Validator[] validators;
  protected boolean shouldRecordContext;
  final boolean recursiveAnchor;

  private final Set<RefSchema> referringSchemas;

  public SchemaImpl(JsonObject schema, JsonPointer scope, MutableStateValidator parent) {
    super(parent);
    this.schema = schema;
    this.scope = scope;
    this.shouldRecordContext = false;
    this.recursiveAnchor = schema.getBoolean("$recursiveAnchor", false);
    referringSchemas = new HashSet<>();
  }

  @Override
  public Future<Void> validateAsync(Object json) {
    return this.validateAsync(new NoopValidatorContext(), json);
  }

  @Override
  public void validateSync(Object json) throws ValidationException, NoSyncValidationException {
    this.validateSync(new NoopValidatorContext(), json);
  }

  @Override
  public JsonPointer getScope() {
    return scope;
  }

  @Override
  public JsonObject getJson() {
    return schema;
  }

  private Object getDefaultValue() {
    return schema.getValue("default");
  }

  @Override
  public Future<Object> getOrApplyDefaultAsync(Object input) {
    if (this.isSync()) {
      return Future.succeededFuture(this.getOrApplyDefaultSync(input));
    }
    if (input == null) {
      return Future.succeededFuture(getDefaultValue());
    }
    return Future.all(
      Arrays.stream(validators)
        .filter(v -> v instanceof DefaultApplier)
        .map(v -> ((DefaultApplier) v).applyDefaultValue(input))
        .collect(Collectors.toList())
    ).map(input);
  }

  @Override
  public Object getOrApplyDefaultSync(Object input) {
    this.checkSync();
    if (input == null) {
      return getDefaultValue();
    }
    for (Validator v : validators) {
      if (v instanceof DefaultApplier) {
        ((DefaultApplier) v).applyDefaultValue(input);
      }
    }
    return input;
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
  public Future<Void> validateAsync(ValidatorContext context, Object in) {
    if (isSync()) return validateSyncAsAsync(context, in);
    if (log.isTraceEnabled())
      log.trace(String.format("Starting async validation for schema %s and input %s", schema, in));

    context = generateValidationContext(context);

    return runAsyncValidators(context, in);
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
    this.checkSync();
    context = generateValidationContext(context);
    runSyncValidator(context, in);
  }

  @Override
  public boolean calculateIsSync() {
    return validators.length == 0 || Arrays.stream(validators).map(Validator::isSync).reduce(true, Boolean::logicalAnd);
  }

  void setValidators(Set<Validator> validators) {
    this.shouldRecordContext = validators
      .stream()
      .map(Validator::getPriority)
      .anyMatch(p -> p == ValidatorPriority.CONTEXTUAL_VALIDATOR);
    this.validators = validators.toArray(new Validator[0]);
    Arrays.sort(this.validators, ValidatorPriority.COMPARATOR);
    this.initializeIsSync();
  }

  void registerReferredSchema(RefSchema ref) {
    referringSchemas.add(ref);
    if (log.isTraceEnabled()) {
      log.trace(String.format("Ref schema %s reefers to schema %s", ref, this));
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

  protected ValidatorContext generateValidationContext(ValidatorContext parent) {
    ValidatorContext context = shouldRecordContext ? parent.startRecording() : parent;
    if (this.recursiveAnchor) {
      return RecursiveAnchorValidatorContextDecorator.wrap(context, this.scope);
    }
    return context;
  }

  protected Future<Void> runAsyncValidators(ValidatorContext context, Object in) {
    List<Future<Void>> futures = new ArrayList<>();
    for (Validator validator : validators) {
      if (!validator.isSync()) {
        Future<Void> asyncValidate = ((AsyncValidator) validator).validateAsync(context, in);
        asyncValidate = asyncValidate.recover(t -> fillException(t, context, in));
        futures.add(asyncValidate);
      } else try {
        ((SyncValidator) validator).validateSync(context, in);
      } catch (ValidationException e) {
        fillValidationException(e, context);
        return Future.failedFuture(e);
      }
    }
    if (!futures.isEmpty()) {
      return Future.all(futures).compose(cf -> Future.succeededFuture());
    } else {
      return Future.succeededFuture();
    }
  }

  protected void runSyncValidator(ValidatorContext context, Object in) {
    for (Validator validator : validators) {
      try {
        ((SyncValidator) validator).validateSync(context, in);
      } catch (ValidationException e) {
        fillValidationException(e, context);
        throw e;
      }
    }
  }

  private void fillValidationException(ValidationException e, ValidatorContext context) {
    if (e.inputScope() == null) { // We need to fill the exception only if it's not filled
      ((ValidationExceptionImpl) e).setSchema(this);
      // Compute the input scope pointer
      List<String> pointerChunks = new ArrayList<>();
      recursiveWalkBackTheValidationContext(context, pointerChunks);
      ((ValidationExceptionImpl) e).setInputScope(JsonPointer.create().append(pointerChunks));
    }
  }

  private Future<Void> fillException(Throwable e, ValidatorContext ctx, Object in) {
    if (e instanceof ValidationException) {
      ValidationException ve = (ValidationException) e;
      fillValidationException(ve, ctx);
      return Future.failedFuture(ve);
    } else {
      return Future.failedFuture(ValidationException.create("Error while validating", null, in, e));
    }
  }

  private void recursiveWalkBackTheValidationContext(ValidatorContext context, List<String> pointerElements) {
    ValidatorContext parent = context.parent();
    if (parent == null) {
      return;
    }
    recursiveWalkBackTheValidationContext(parent, pointerElements);
    pointerElements.add(context.inputKey());
  }
}
