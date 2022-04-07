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
package io.vertx.json.schema.draft202012;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.*;
import io.vertx.json.schema.draft201909.*;
import io.vertx.json.schema.draft201909.ContainsValidatorFactory;
import io.vertx.json.schema.draft201909.FormatValidatorFactory;
import io.vertx.json.schema.draft7.*;
import io.vertx.json.schema.draft7.ItemsValidatorFactory;

import java.net.URI;
import java.util.*;

public class Draft202012SchemaParser extends Draft201909SchemaParser {

  protected Draft202012SchemaParser(SchemaRouter router) {
    super(router);
  }

  @Override
  protected List<ValidatorFactory> initValidatorFactories() {
    List<ValidatorFactory> factories = new LinkedList<>();
    factories.add(new DefinitionsValidatorFactory("$defs"));
    factories.add(new FormatValidatorFactory());
    factories.add(new MaximumValidatorFactory());
    factories.add(new MinimumValidatorFactory());
    factories.add(new ContainsValidatorFactory());
    factories.add(new ConstValidatorFactory());
    factories.add(new TypeValidatorFactory());
    factories.add(new AllOfValidatorFactory());
    factories.add(new AnyOfValidatorFactory());
    factories.add(new EnumValidatorFactory());
    factories.add(new PrefixItemsValidatorFactory());
    factories.add(new ItemsValidatorFactory());
    factories.add(new MaxItemsValidatorFactory());
    factories.add(new MaxLengthValidatorFactory());
    factories.add(new MaxPropertiesValidatorFactory());
    factories.add(new MinItemsValidatorFactory());
    factories.add(new MinLengthValidatorFactory());
    factories.add(new MinPropertiesValidatorFactory());
    factories.add(new MultipleOfValidatorFactory());
    factories.add(new NotValidatorFactory());
    factories.add(new OneOfValidatorFactory());
    factories.add(new PatternValidatorFactory(true));
    factories.add(new PropertiesValidatorFactory());
    factories.add(new RequiredValidatorFactory());
    factories.add(new UniqueItemsValidatorFactory());
    factories.add(new DependentSchemasValidatorFactory());
    factories.add(new DependentRequiredValidatorFactory());
    factories.add(new ExclusiveMaximumValidatorFactory());
    factories.add(new ExclusiveMinimumValidatorFactory());
    factories.add(new IfThenElseValidatorFactory());
    factories.add(new PropertyNamesValidatorFactory());
    factories.add(new UnevaluatedItemsValidatorFactory());
    factories.add(new UnevaluatedPropertiesValidatorFactory());
    return factories;
  }

  @Override
  public Draft202012SchemaParser ignoreFormatKeyword() {
    super.ignoreFormatKeyword();
    return this;
  }

  /**
   * Instantiate a Draft202012SchemaParser
   *
   * @param router router to associate to read $ref
   * @return a new instance of Draft202012SchemaParser
   */
  public static Draft202012SchemaParser create(SchemaRouter router) {
    return new Draft202012SchemaParser(router);
  }

  /**
   * Parse a draft2020-12 schema
   *
   * @param vertx  this vertx instance
   * @param schema parsed json schema
   * @param scope  scope of json schema
   * @return a new instance of Draft202012SchemaParser
   * @throws SchemaException if schema is invalid
   */
  public static Schema parse(Vertx vertx, JsonObject schema, URI scope) {
    return new Draft202012SchemaParser(SchemaRouter.create(vertx, new SchemaRouterOptions())).parse(schema, scope);
  }
}
