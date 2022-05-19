package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaException;

import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;
import static io.vertx.json.schema.impl.Utils.Objects.isObject;

public final class Ref {

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

  Ref(String ref, JsonObject obj, String prop, String path, String id) {
    this.ref = ref;
    this.obj = obj;
    this.prop = prop;
    this.path = path;
    this.id = id;
  }

  public static JsonObject resolve(Map<String, JsonSchema> refs, URL baseUri, JsonSchema schema) {
    final JsonObject tree = ((JsonObject) schema).copy();
    final Map<String, List<Ref>> pointers = new HashMap<>();

    // find all refs
    findRefsAndClean(tree, "#", "", pointers);

    // resolve them
    final Map<String, JsonObject> anchors = new HashMap<>();
    anchors.put("", tree);

    final JsonObject dynamicAnchors = new JsonObject();

    pointers
      .computeIfAbsent("$id", key -> Collections.emptyList())
      .forEach(item -> {
        final String ref = item.ref;
        final String path = item.path;
        final JsonObject obj = item.obj;
        if (anchors.containsKey(ref)) {
          throw new SchemaException(schema, "$id: '" + ref + "' defined more than once at: " + path);
        }
        anchors.put(ref, obj);
      });

    pointers
      .computeIfAbsent("$anchor", key -> Collections.emptyList())
      .forEach(item -> {
        final String ref = item.ref;
        final String path = item.path;
        final JsonObject obj = item.obj;
        final String id = item.id;

        final String fullRef = id + "#" + ref;

        if (anchors.containsKey(fullRef)) {
          throw new SchemaException(schema, "$anchor: '" + ref + "' defined more than once at: " + path);
        }
        anchors.put(fullRef, obj);
      });

    pointers
      .computeIfAbsent("$dynamicAnchor", key -> Collections.emptyList())
      .forEach(item -> {
        final String ref = item.ref;
        final String path = item.path;
        final JsonObject obj = item.obj;

        if (dynamicAnchors.containsKey("#" + ref)) {
          throw new SchemaException(schema, "$dynamicAnchor: '" + ref + "' defined more than once at: " + path);
        }
        dynamicAnchors.put("#" + ref, obj);
      });

    pointers
      .computeIfAbsent("$ref", key -> Collections.emptyList())
      .forEach(item -> {
        final String ref = item.ref;
        final String prop = item.prop;
        final JsonObject obj = item.obj;
        final String id = item.id;

        obj.remove(prop);

        final String decodedRef = decodeURIComponent(ref);
        final String fullRef = decodedRef.charAt(0) != '#' ? decodedRef : id + decodedRef;
        // re-assign the obj
        obj.mergeIn(
          new JsonObject(
            resolveUri(refs, baseUri, schema, fullRef, anchors)
              // filter out pointer keywords
              .stream()
              .filter(kv -> !POINTER_KEYWORD.contains(kv.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
      });

    pointers
      .computeIfAbsent("$dynamicRef", key -> Collections.emptyList())
      .forEach(item -> {
        final String ref = item.ref;
        final String prop = item.prop;
        final JsonObject obj = item.obj;
        if (!dynamicAnchors.containsKey(ref)) {
          throw new SchemaException(schema, "Can't resolve $dynamicAnchor: '" + ref + "'");
        }
        obj.remove(prop);
        // re-assign the obj
        obj.mergeIn(
          new JsonObject(
            dynamicAnchors.getJsonObject(ref)
              // filter out pointer keywords
              .stream()
              .filter(kv -> !POINTER_KEYWORD.contains(kv.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
      });

    return tree;
  }

  private static void findRefsAndClean(Object obj, String path, String id, Map<String, List<Ref>> pointers) {
    if (!isObject(obj)) {
      return;
    }
    if (obj instanceof JsonArray) {
      final JsonArray json = (JsonArray) obj;
      // process the array
      for (int i = 0; i < json.size(); i++) {
        findRefsAndClean(json.getValue(i), path + "/" + i, id, pointers);
      }
    }

    if (obj instanceof JsonObject) {
      final JsonObject json = (JsonObject) obj;

      // clean up annotations
      json.remove("__absolute_uri__");
      json.remove("__absolute_ref__");
      json.remove("__absolute_recursive_ref__");

      if (json.containsKey("$id")) {
        id = json.getString("$id");
      }

      // process the object
      for (String prop : json.fieldNames()) {
        if (POINTER_KEYWORD.contains(prop)) {
          pointers
            .computeIfAbsent(prop, key -> new ArrayList<>())
            .add(new Ref(json.getString(prop), json, prop, path, id));
        }
        findRefsAndClean(json.getValue(prop), path + "/" + Utils.Pointers.encode(prop), id, pointers);
      }
    }
  }

  private static JsonObject resolveUri(Map<String, JsonSchema> refs, URL baseUri, JsonSchema schema, String uri, Map<String, JsonObject> anchors) {
    //  [prefix, path]
    final String[] parts = uri.split("#", 2);

    final boolean hashPresent = parts.length == 2 && parts[1] != null;

    final String prefix = parts[0];
    final String path = hashPresent ? parts[1] : null;

    if (hashPresent && (path.charAt(0) != '/')) {
      if (anchors.containsKey(uri)) {
        return anchors.get(uri);
      }
      throw new SchemaException(schema, "Can't resolve '" + uri + "', only internal refs are supported.");
    }

    if (!anchors.containsKey(prefix)) {
      if (!refs.isEmpty()) {
        String resolved = new URL(prefix, baseUri).href();
        if (refs.containsKey(resolved)) {
          return resolve(refs, baseUri, refs.get(resolved));
        }
      }
      throw new SchemaException(schema, "Can't resolve '" + uri + "', only internal refs are supported.");
    }

    if (!hashPresent) {
      return anchors.get(prefix);
    }

    final String[] paths = path.split("/");
    JsonObject value = anchors.get(prefix).copy();
    // perform a reduce operation
    for (int i = 1; i < paths.length; i++) {
      value = value
        .getJsonObject(Utils.Pointers.unescape(paths[i]));

      if (value == null) {
        throw new SchemaException(schema, "Can't resolve '" + uri + "', only internal refs are supported.");
      }
    }
    return value;
  }
}
