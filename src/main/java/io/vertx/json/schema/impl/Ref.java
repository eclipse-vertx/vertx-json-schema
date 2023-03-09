package io.vertx.json.schema.impl;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaException;

import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;
import static io.vertx.json.schema.impl.Utils.Objects.isObject;

public final class Ref {

  private static final Logger LOG = LoggerFactory.getLogger(Ref.class);

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

    try {
      // find all refs
      findRefsAndClean(tree, "#", "", pointers);

      // resolve them
      final Map<String, JsonObject> anchors = new HashMap<>();
      anchors.put("", tree);

      final JsonObject dynamicAnchors = new JsonObject();

      boolean incomplete = false;

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

      final Map<String, JsonObject> dejaVu = new HashMap<>();

      for (Ref item : pointers.computeIfAbsent("$ref", key -> Collections.emptyList())) {
        final String ref = item.ref;
        final String prop = item.prop;
        final JsonObject obj = item.obj;
        final String id = item.id;

        final String decodedRef = decodeURIComponent(ref);
        final String fullRef = decodedRef.charAt(0) != '#' ? decodedRef : id + decodedRef;

        // resolve is expensive, so we cache the result
        JsonObject resolved = dejaVu.computeIfAbsent(fullRef, key -> resolveUri(refs, baseUri, schema, key, anchors));
        // resolved may contain pointers which means we need to resolve them too
        incomplete |= hasPointers(resolved, "", fullRef);

        obj.remove(prop);

        // re-assign the obj
        obj.mergeIn(
          new JsonObject(
            resolved
              // filter out pointer keywords
              .stream()
              .filter(kv -> !POINTER_KEYWORD.contains(kv.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
      }

      for (Ref item : pointers.computeIfAbsent("$dynamicRef", key -> Collections.emptyList())) {
        final String ref = item.ref;
        final String prop = item.prop;
        final JsonObject obj = item.obj;
        if (!dynamicAnchors.containsKey(ref)) {
          throw new SchemaException(schema, "Can't resolve $dynamicAnchor: '" + ref + "'");
        }

        JsonObject resolved = dynamicAnchors.getJsonObject(ref);
        // resolved may contain pointers which means we need to resolve them too
        incomplete |= hasPointers(resolved, "", ref);

        obj.remove(prop);
        // re-assign the obj
        obj.mergeIn(
          new JsonObject(
            resolved
              // filter out pointer keywords
              .stream()
              .filter(kv -> !POINTER_KEYWORD.contains(kv.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
      }

      if (incomplete) {
        // the schema changed we need to re-run
        return resolve(refs, baseUri, JsonSchema.of(tree));
      } else {
        return tree;
      }
    } catch (CircularReferenceException e) {
      LOG.debug(e.getMessage());
      return tree;
    }
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

  private static boolean hasPointers(Object obj, String parent, String self) throws CircularReferenceException {
    if (!isObject(obj)) {
      return false;
    }
    if (obj instanceof JsonArray) {
      final JsonArray json = (JsonArray) obj;
      // process the array
      for (int i = 0; i < json.size(); i++) {
        if (hasPointers(json.getValue(i), parent + "/" + i, self)) {
          return true;
        }
      }
    }
    if (obj instanceof JsonObject) {
      final JsonObject json = (JsonObject) obj;
      // process the object
      for (String prop : json.fieldNames()) {
        if (POINTER_KEYWORD.contains(prop)) {
          // json-schema allows circular pointers, this is problematic
          // as we cannot resolve those (stack overflow) so we need to
          // accept that these won't be resolved and remain purely as references
          if (json.getString(prop).equals(self)) {
            throw new CircularReferenceException("Circular pointer detected: '" + self + "' in schema [" + self + parent + "/" + prop + "].");
          } else {
            return true;
          }
        } else {
          if (hasPointers(json.getValue(prop), parent + "/" + prop, self)) {
            return true;
          }
        }
      }
    }
    return false;
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
          // if there is no hash we can safely return the full object
          if (!hashPresent) {
            return resolve(refs, baseUri, refs.get(resolved));
          }
          // in case of hash we need to reduce...
          return reduce(schema, path, resolve(refs, baseUri, refs.get(resolved)));
        }
      }
      throw new SchemaException(schema, "Can't resolve '" + uri + "', only internal refs are supported.");
    }

    // if there is no hash we can safely return the full object
    if (!hashPresent) {
      return anchors.get(prefix);
    }

    // in case of hash we need to reduce...
    return reduce(schema, path, anchors.get(prefix));
  }

  private static JsonObject reduce(JsonSchema schema, String path, JsonObject value) {
    final String[] paths = path.split("/");
    // perform a reduce operation
    for (int i = 1; i < paths.length; i++) {
      value = value
        .getJsonObject(Utils.Pointers.unescape(paths[i]));

      if (value == null) {
        throw new SchemaException(schema, "Can't reduce [" + i + "] '" + path + "', value is null.");
      }
    }
    // work with a copy to avoid mutations
    return value.copy();
  }
}
