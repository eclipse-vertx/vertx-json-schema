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

import io.vertx.core.json.pointer.JsonPointer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class URIUtils {

  public static URI removeFragment(URI oldURI) {
    return URIUtils.replaceFragment(oldURI, null);
  }

  public static URI replaceFragment(URI oldURI, String fragment) {
    try {
      if (oldURI != null) {
        return new URI(oldURI.getScheme(), oldURI.getSchemeSpecificPart(), fragment);
      } else return new URI(null, null, fragment);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static boolean isRemoteURI(URI uri) {
    return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
  }

  public static boolean isLocalURI(URI uri) {
    return "jar".equals(uri.getScheme()) || "file".equals(uri.getScheme());
  }

  public static URI resolvePath(URI oldURI, String path) {
    try {
      if ("jar".equals(oldURI.getScheme())) {
        String[] splittedJarURI = oldURI.getSchemeSpecificPart().split("!");
        String newInternalJarPath = URI.create(splittedJarURI[1]).resolve(path).toString();
        return new URI(oldURI.getScheme(), splittedJarURI[0] + "!" + newInternalJarPath, oldURI.getFragment());
      } else if (path.isEmpty()) return oldURI;
      else return oldURI.resolve(path);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * This function converts eventual "#a" to valid json pointer "#/a"
   *
   * @param original
   * @return
   */
  public static JsonPointer createJsonPointerFromURI(URI original) {
    String frag = original.getFragment();
    if (frag != null && !frag.isEmpty()) {
      if (frag.charAt(0) != '/') frag = "/" + frag;
    }
    return JsonPointer.fromURI(replaceFragment(original, frag));
  }

  public static URI requireAbsoluteUri(URI uri) {
    Objects.requireNonNull(uri);
    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("Provided uri should be absolute. Actual: " + uri.toString());
    }
    return uri;
  }

  public static URI requireAbsoluteUri(URI uri, String name) {
    Objects.requireNonNull(uri);
    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("Provided " + name + " uri should be absolute. Actual: " + uri.toString());
    }
    return uri;
  }

}
