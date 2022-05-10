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
package io.vertx.json.schema;

import io.vertx.codegen.annotations.VertxGen;

/**
 * Json-Schema defines how validators should output the validation result for interop.
 *
 * @author Paulo Lopes
 */
@VertxGen
public enum OutputFormat {
  /**
   * Short circuit output, just a simple {@code true/false} value.
   */
  Flag,

  /**
   * Flat list of errors and annotations with boolean success value.
   */
  Basic,

// TODO: bellow are not supported yet
//  Detailed,
//  Verbose
}
