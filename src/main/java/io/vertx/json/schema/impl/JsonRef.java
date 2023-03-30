package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

import java.util.*;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;
import static io.vertx.json.schema.impl.Utils.Objects.isObject;

public final class JsonRef {

  public static final List<String> POINTER_KEYWORD = Arrays.asList(
    "$ref",
    "$id",
    "$anchor",
    "$dynamicRef",
    "$dynamicAnchor",
    "$schema"
  );

  final String ref;
  final JsonObject obj;
  final String prop;
  final String path;
  final String id;

  JsonRef(String ref, JsonObject obj, String prop, String path, String id) {
    this.ref = ref;
    this.obj = obj;
    this.prop = prop;
    this.path = path;
    this.id = id;
  }

  public static JsonObject resolve(JsonObject schema) {
    return resolve(schema, Collections.emptyMap());
  }

  public static JsonObject resolve(JsonObject schema, Map<String, JsonSchema> lookup) {
    if (!isObject(schema)) {
      return null;
    }

    // work with a copy as the internals of the object will be modified
    JsonObject tree = schema.copy();
    final Map<String, List<JsonRef>> pointers = new HashMap<>();

    // find all refs
    parse(tree, "#", "", pointers);

    // resolve them
    final Map<String, JsonObject> anchors = new HashMap<>();
    anchors.put("", tree);

    final Map<String, JsonObject> dynamicAnchors = new HashMap<>();

    for (final JsonRef item : pointers.computeIfAbsent("$id", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      final JsonObject obj = item.obj;
      if (anchors.containsKey(ref)) {
        throw new IllegalStateException("$id: '" + ref + "' defined more than once at: " + path);
      }
      anchors.put(ref, obj);
    }

    for (final JsonRef item : pointers.computeIfAbsent("$anchor", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      final JsonObject obj = item.obj;
      final String id = item.id;

      final String fullRef = id + "#" + ref;

      if (anchors.containsKey(fullRef)) {
        throw new IllegalStateException("$anchor: '" + ref + "' defined more than once at: " + path);
      }
      anchors.put(fullRef, obj);
    }

    for (final JsonRef item : pointers.computeIfAbsent("$dynamicAnchor", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      final JsonObject obj = item.obj;

      if (dynamicAnchors.containsKey("#" + ref)) {
        throw new IllegalStateException("$dynamicAnchor: '" + ref + "' defined more than once at: " + path);
      }
      dynamicAnchors.put("#" + ref, obj);
    }

    for (final JsonRef item : pointers.computeIfAbsent("$ref", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String id = item.id;
      final String path = item.path;

      final String decodedRef = decodeURIComponent(ref);
      final String fullRef = decodedRef.charAt(0) != '#' ? decodedRef : id + decodedRef;

      tree = applyRef(tree, path, resolveUri(fullRef, anchors, lookup));
    }

    for (JsonRef item : pointers.computeIfAbsent("$dynamicRef", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      if (!dynamicAnchors.containsKey(ref)) {
        throw new IllegalStateException("Can't resolve $dynamicAnchor: '" + ref + "'");
      }

      tree = applyRef(tree, path, dynamicAnchors.get(ref));
    }

    return tree;
  }

  private static void parse(Object obj, String path, String id, Map<String, List<JsonRef>> pointers) {
    System.out.println("parse: " + path + " " + id);

    if (!isObject(obj)) {
      return;
    }

    if (obj instanceof JsonArray) {
      final JsonArray json = (JsonArray) obj;
      // process the array
      for (int i = 0; i < json.size(); i++) {
        parse(json.getValue(i), path + "/" + i, id, pointers);
      }
    }

    if (obj instanceof JsonObject) {
      final JsonObject json = (JsonObject) obj;

      if (json.containsKey("$id")) {
        id = json.getString("$id");
      }

      // process the object
      Iterator<String> iterator = json.fieldNames().iterator();
      while (iterator.hasNext()) {
        final String prop = iterator.next();
        // skip the special properties
        switch (prop) {
          case "__absolute_uri__":
          case "__absolute_ref__":
          case "__absolute_recursive_ref__":
            continue;
        }
        if (POINTER_KEYWORD.contains(prop)) {
          pointers
            .computeIfAbsent(prop, key -> new ArrayList<>())
            .add(new JsonRef(json.getString(prop), json, prop, path, id));
          // remove the prop so we don't process it again
          iterator.remove();
        }
        parse(json.getValue(prop), path + "/" + Utils.Pointers.encode(prop), id, pointers);
      }
    }
  }

  private static JsonObject applyRef(JsonObject tree, String path, JsonObject target) {
    System.out.println("applyRef: " + path);

    // root can be JsonObject or JsonArray
    Object root = tree;
    final String[] paths = path.split("/");
    final String prop;
    if (paths.length > 1) {
      prop = paths[paths.length - 1];
      for (int i = 1; i < paths.length - 1; i++) {
        final String p = paths[i];
        if (root instanceof JsonArray) {
          root = ((JsonArray) root).getValue(Integer.parseInt(p));
        } else if (root instanceof JsonObject) {
          root = ((JsonObject) root).getValue(Utils.Pointers.unescape(p));
        }
      }

      // replace
      if (root instanceof JsonArray) {
        ((JsonArray) root).set(Integer.parseInt(prop), target);
      } else if (root instanceof JsonObject) {
        ((JsonObject) root).put(Utils.Pointers.unescape(prop), target);
      }

      return tree;
    } else {
      // undefined
      return target;
    }
  }

  private static JsonObject resolveUri(String uri, Map<String, JsonObject> anchors, Map<String, JsonSchema> lookup) {
    //  [prefix, path]
    final String[] parts = uri.split("#", 2);

    final boolean hashPresent = parts.length == 2 && parts[1] != null;

    final String prefix = parts[0];
    final String path = hashPresent ? parts[1] : null;

    if (hashPresent && (path.charAt(0) != '/')) {
      if (anchors.containsKey(uri)) {
        return anchors.get(uri);
      }
      throw new UnsupportedOperationException("Can't resolve '" + uri + "', only internal refs are supported.");
    }

    if (!anchors.containsKey(prefix)) {

      if (lookup.containsKey(prefix)) {
        // if there is no hash we can safely return the full object
        if (!hashPresent) {
          return resolve((JsonObject) lookup.get(prefix), lookup);
        }
        // in case of hash we need to reduce...
        return reduce(path, resolve((JsonObject) lookup.get(prefix), lookup));
      }

      throw new UnsupportedOperationException("Can't resolve '" + uri + "', only internal refs are supported.");
    }

    // if there is no hash we can safely return the full object
    if (!hashPresent) {
      return anchors.get(prefix);
    }

    // in case of hash we need to reduce...
    return reduce(path, anchors.get(prefix));
  }

  private static JsonObject reduce(final String path, final JsonObject initialValue) {
    final String[] paths = path.split("/");
    // perform a reduce operation
    JsonObject accumulator = initialValue;
    // skip the first element as it is the current object
    for (int i = 1; i < paths.length; i++) {
      String currentValue = Utils.Pointers.unescape(paths[i]);
      if (accumulator.containsKey(currentValue)) {
        accumulator = accumulator.getJsonObject(currentValue);
      } else {
        throw new IllegalStateException("Can't reduce [" + i + "] '" + path + "', value is undefined.");
      }
    }
    return accumulator;
  }
}
