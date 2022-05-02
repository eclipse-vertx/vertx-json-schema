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
 * Json-Schema draft.
 *
 * The enum does not explicitly define all known drafts but a selection of the most widely used ones and supported
 * in the implementation.
 *
 * @author Paulo Lopes
 */
@VertxGen
public enum Draft {
  /**
   * Draft 4 - <a href="http://json-schema.org/draft-04/schema#">http://json-schema.org/draft-04/schema#</a>
   *
   * Usually used by Swagger 2.0
   */
  DRAFT4,

  /**
   * Draft 7 - <a href="http://json-schema.org/draft-07/schema#">http://json-schema.org/draft-07/schema#</a>
   *
   * Usually used by OpenAPI 3.0
   */
  DRAFT7,

  /**
   * Draft 2019-09 - <a href="https://json-schema.org/draft/2019-09/schema">https://json-schema.org/draft/2019-09/schema</a>
   *
   * Commonly used by many projects
   */
  DRAFT201909,

  /**
   * Draft 2019-09 - <a href="https://json-schema.org/draft/2020-12/schema">https://json-schema.org/draft/2020-12/schema</a>
   *
   * Usually used by OpenAPI 3.1
   */
  DRAFT202012;

  /**
   * Converts a draft number to a {@link Draft} enum value.
   * @param string A draft number, e.g.: {@code [4|7|8|2019-09|2020-12]}
   * @return a Draft enum value
   */
  public static Draft from(String string) {
    if (string == null) {
      throw new IllegalArgumentException("Invalid draft: null");
    }
    switch (string) {
      case "4":
        return DRAFT4;
      case "7":
        return DRAFT7;
      case "8":
      case "2019-09":
        return DRAFT201909;
      case "2020-12":
        return DRAFT202012;
      default:
        throw new IllegalArgumentException("Unsupported draft: " + string);
    }
  }

  /**
   * Converts a draft idenfifier to a {@link Draft} enum value.
   * @param string The identifier (in URL format)
   * @return a Draft enum value
   */
  public static Draft fromIdentifier(String string) {
    if (string == null) {
      throw new IllegalArgumentException("Invalid draft identifier: null");
    }
    switch (string) {
      case "http://json-schema.org/draft-04/schema#":
        return DRAFT4;
      case "http://json-schema.org/draft-07/schema#":
        return DRAFT7;
      case "https://json-schema.org/draft/2019-09/schema":
        return DRAFT201909;
      case "https://json-schema.org/draft/2020-12/schema":
        return DRAFT202012;
      default:
        throw new IllegalArgumentException("Unsupported draft identifier: " + string);
    }
  }
}
