package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaException;

import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;
import static io.vertx.json.schema.impl.Utils.Objects.isObject;

public final class JsonObjectSchema extends JsonObject implements JsonSchema {

  private static final List<String> POINTER_KEYWORD = Arrays.asList(
    "$ref",
    "$id",
    "$anchor",
    "$dynamicRef",
    "$dynamicAnchor",
    "$schema"
  );

  private boolean annotated;

  public JsonObjectSchema(JsonObject json) {
    super(json.getMap());
    // inherit the annotated flag
    this.annotated =
      json.containsKey("__absolute_uri__") ||
      json.containsKey("__absolute_ref__") ||
      json.containsKey("__absolute_recursive_ref__");
  }

  @Override
  public JsonSchema annotate(String key, String value) {
    switch (key) {
      case "__absolute_uri__":
        annotated = true;
        put("__absolute_uri__", value);
        break;
      case "__absolute_ref__":
        annotated = true;
        put("__absolute_ref__", value);
        break;
      case "__absolute_recursive_ref__":
        annotated = true;
        put("__absolute_recursive_ref__", value);
        break;
      default:
        throw new IllegalArgumentException("Unsupported annotation: " + key);
    }
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key, R fallback) {
    return (R) getValue(key, fallback);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key) {
    return (R) getValue(key);
  }

  @Override
  public Set<String> fieldNames() {
    if (annotated) {
      // filter out the annotations
      Set<String> filteredFieldNames = new HashSet<>(super.fieldNames());
      filteredFieldNames.remove("__absolute_uri__");
      filteredFieldNames.remove("__absolute_ref__");
      filteredFieldNames.remove("__absolute_recursive_ref__");
      return filteredFieldNames;
    } else {
      return super.fieldNames();
    }
  }

  @Override
  public JsonObject resolve() {
    final JsonObject tree = this.copy();

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
          throw new SchemaException(this, "$id: '" + ref + "' defined more than once at: " + path);
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
          throw new SchemaException(this, "$anchor: '" + ref + "' defined more than once at: " + path);
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
          throw new SchemaException(this, "$dynamicAnchor: '" + ref + "' defined more than once at: " + path);
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
            resolveUri(this, fullRef, anchors)
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
          throw new SchemaException(this, "Can't resolve $dynamicAnchor: '" + ref + "'");
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

  private static JsonObject resolveUri(JsonSchema schema, String uri, Map<String, JsonObject> anchors) {
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

  private static final class Ref {
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
  }

  private static void findRefsAndClean(Object obj, String path, String id, Map<String, List<Ref>> pointers) {
    if (!isObject(obj)) {
      return;
    }
    if (obj instanceof JsonObject) {
      final JsonObject json = (JsonObject) obj;

      // clean up annotations
      json.remove("__absolute_uri__");
      json.remove("__absolute_ref__");
      json.remove("__absolute_recursive_ref__");

      // compute the id (according to draft-4 or later)
      if (json.containsKey("$id") || json.containsKey("id")) {
        id = json.getString("$id", json.getString("id"));
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
}
