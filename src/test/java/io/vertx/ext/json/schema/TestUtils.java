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
package io.vertx.ext.json.schema;

import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;


public class TestUtils {

  public static JsonObject loadJson(URI uri) throws IOException {
    return new JsonObject(String.join("", Files.readAllLines(Paths.get(uri))));
  }

  public static URI buildBaseUri(String... filename) {
    return Paths.get("src", "test").resolve(Paths.get("resources", filename)).toAbsolutePath().toUri();
  }

  public static AbstractMap.SimpleImmutableEntry<String, Object> entry(String key, Object value) {
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

}
