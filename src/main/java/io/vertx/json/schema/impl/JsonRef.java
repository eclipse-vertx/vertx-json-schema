package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

import java.util.*;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;
import static io.vertx.json.schema.impl.Utils.Objects.isObject;

/**
 * This class is used to resolve JSON references. Resolving means replacing all the references with the actual object.
 * While in many cases this is a safe operation, in some cases it's not. For example, if you have a schema like this:
 *
 * <pre>
 *   {
 *   "type": "object",
 *   "properties": {
 *     "hello": {
 *       "$ref": "#/definitions/hello"
 *     }
 *   },
 *   "default": {
 *     "hello": {
 *       "name": "francesco"
 *     }
 *   },
 *   "definitions": {
 *     "hello": {
 *       "type": "object",
 *       "properties": {
 *         "name": {
 *           "type": "string",
 *           "default": "world"
 *         },
 *         "and": {
 *           "$ref": "#"
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * In this case while attempting to resolve the reference to the definition of "hello" we will find a reference to the
 * root schema, which is allowed. The fact that it is allowed it means we can enter an infinite cycle, so we need to
 * actually replace the reference with the actual object.
 */
public final class JsonRef {

  /**
   * This is a list of keys that are allowed to be used as a pointer, they usually point to a json pointer.
   */
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

  /**
   * A JsonRef internal instance is a helper to track the path of the reference.
   */
  private JsonRef(String ref, JsonObject obj, String prop, String path, String id) {
    this.ref = ref;
    this.obj = obj;
    this.prop = prop;
    this.path = path;
    this.id = id;
  }

  /**
   * Public API to resolve a schema. This method will return a new schema with all the references resolved. The
   * resolution is not done in-place, so the original schema will be copied and a modified schema JSON object is
   * returned.
   */
  public static JsonObject resolve(JsonObject schema) {
    return resolve(schema, Collections.emptyMap());
  }

  private static <T> T copy(T o) {
    if (o instanceof JsonObject) {
      JsonObject obj = (JsonObject) o;
      JsonObjectProxy ret = new JsonObjectProxy();
      for (Map.Entry<String, ?> child : obj) {
        ret.put(child.getKey(), copy(child.getValue()));
      }
      return (T) ret;
    } else if (o instanceof JsonArray) {
      // Todo : use JsonArrayProxy
      JsonArray obj = (JsonArray) o;
      JsonArray ret = new JsonArray();
      for (Object elt : obj) {
        ret.add(copy(elt));
      }
      return (T) ret;
    } else {
      return o;
    }
  }

  /**
   * Public API to resolve a schema like {@link #resolve(JsonObject)}. The main difference is that there is context
   * to lookup external schemas. External schemas are only looked up for absolute references.
   *
   * {@see #resolve(JsonObject)}
   */
  public static JsonObject resolve(JsonObject schema, Map<String, JsonSchema> lookup) {
    // the algorithm to resolve a schema is as follows:

    // 1. If a schema is not a JSON object, return null.
    if (!isObject(schema)) {
      return null;
    }

    // 2. work with a copy as the internals of the object will be modified
    JsonObject tree = copy(schema);

    // 3. For each kind of "POINTER_KEYWORD" we will collect them in a map (this is actually a MultiMap)
    final Map<String, List<JsonRef>> pointers = new HashMap<>();

    // 4. Parsing is doing a recursive object graph traversal and collecting all pointers into the multimap.
    //    It is important to notice that the parse method isn't looking for circular references itself, so
    //    it's possible to have a circular reference and a StackOverflowError will be thrown.
    parse(tree, "#", "", pointers);

    // 5. Start the resolve process. For each resolved reference, we will have an "anchor". Initially this
    //    anchor map contains the root schema as the map holds the resolved references without the "#" prefix.
    final Map<String, JsonObject> anchors = new HashMap<>();
    anchors.put("", tree);

    // 5.1. From JsonSchema draft 2019-09, the $dynamicAnchor keyword is used to define a dynamic anchor. This
    //      is a special kind of anchor that is not resolved at the time of definition.
    final Map<String, JsonObject> dynamicAnchors = new HashMap<>();

    // 6. The resolve process is done in two steps. First we will resolve all the $id and $anchor keywords.

    // 6.1. Resolve all the $id keywords. This is done by iterating over the collected pointers and for each
    //      $id keyword we will add the resolved reference to the anchors map. However, if the reference is
    //      already present in the map, an exception is thrown. Because it means we have a duplicate $id.
    for (final JsonRef item : pointers.computeIfAbsent("$id", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      final JsonObject obj = item.obj;
      if (anchors.containsKey(ref)) {
        throw new IllegalStateException("$id: '" + ref + "' defined more than once at: " + path);
      }
      anchors.put(ref, obj);
    }

    // 6.2. Resolve all the $anchor keywords. This is done by iterating over the collected pointers and for each
    //      $anchor keyword we will add the resolved reference to the anchors map. However, if the reference is
    //      already present in the map, an exception is thrown. Because it means we have a duplicate $anchor.
    //      Anchors are relative so the reference is the pair of the current id and the anchor name.
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

    // 6.3. Resolve all the $dynamicAnchor keywords. This is done by iterating over the collected pointers and for each
    //      $dynamicAnchor keyword we will add the resolved reference to the dynamicAnchors map. However, if the
    //      reference is already present in the map, an exception is thrown. Because it means we have a duplicate
    //      $dynamicAnchor.
    for (final JsonRef item : pointers.computeIfAbsent("$dynamicAnchor", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      final JsonObject obj = item.obj;

      if (dynamicAnchors.containsKey("#" + ref)) {
        throw new IllegalStateException("$dynamicAnchor: '" + ref + "' defined more than once at: " + path);
      }
      dynamicAnchors.put("#" + ref, obj);
    }

    // 7. The second step is to resolve all the $ref and $dynamicRef keywords.

    // 7.1. This is done by iterating over the initially collected references during the parse step. For each $ref
    //      keyword we will resolve the reference.
    //      Resolve Uri, means that if a reference is a complex object, we will walk the graph to the right location.
    //      When the lookup isn't null we may try to look up absolute references too. When a resolveUri fails we have
    //      an incomplete schema and an exception is thrown.
    //      When the reference is resolved, we will apply the reference to the tree. This is done by walking the schema
    //      to the location where the reference is and replace that object by the resolved java object instance.
    //      the tree itself can be modified as the resolve process is done in-place and a reference can refer to "#"
    //      which is the root of the schema.
    for (final JsonRef item : pointers.computeIfAbsent("$ref", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String id = item.id;
      final String path = item.path;

      final String decodedRef = decodeURIComponent(ref);
      final String fullRef = decodedRef.charAt(0) != '#' ? decodedRef : id + decodedRef;

      tree = applyRef(tree, path, resolveUri(fullRef, anchors, lookup));
    }

    // 7.2. This is done by iterating over the initially collected references during the parse step. For each
    //      $dynamicRef keyword we will resolve the reference. Resolving in this case is a simple map lookup.
    //      When the reference is resolved, we will apply the reference to the tree. This is done by walking the schema
    //      to the location where the reference is and replace that object by the resolved java object instance.
    //      the tree itself can be modified as the resolve process is done in-place and a reference can refer to "#"
    //      which is the root of the schema.
    for (JsonRef item : pointers.computeIfAbsent("$dynamicRef", key -> Collections.emptyList())) {
      final String ref = item.ref;
      final String path = item.path;
      if (!dynamicAnchors.containsKey(ref)) {
        throw new IllegalStateException("Can't resolve $dynamicAnchor: '" + ref + "'");
      }

      tree = applyRef(tree, path, dynamicAnchors.get(ref));
    }

    // As this moment we will have a fully resolved schema which can include circular references. So it is not advisable
    // to use tree.encode() to get the json representation of the schema.
    return tree;
  }

  private static void parse(Object obj, String path, String id, Map<String, List<JsonRef>> pointers) {
    // System.out.println("parse: " + path + " " + id);

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
        parse(json.getValue(prop), path + "/" + Utils.Pointers.escape(prop), id, pointers);
      }
    }
  }

  private static JsonObject applyRef(JsonObject tree, String path, JsonObject target) {
    // System.out.println("applyRef: " + path);

    // root can be JsonObject or JsonArray
    Object root = tree;
    final String[] paths = path.split("/");
    final String prop;
    if (paths.length > 1) {
      prop = paths[paths.length - 1];
      for (int i = 1; i < paths.length - 1; i++) {
        final String p = paths[i];
        // System.out.println("applyRef: walk[" + p + "] tree");

        if (root instanceof JsonArray) {
          root = ((JsonArray) root).getValue(Integer.parseInt(p));
        } else if (root instanceof JsonObject) {
          root = ((JsonObject) root).getValue(Utils.Pointers.unescape(p));
        }
      }

      // replace
      // System.out.println("applyRef: update root[" + prop + "] " + target.fieldNames());

      if (root instanceof JsonArray) {
        ((JsonArray) root).set(Integer.parseInt(prop), new JsonObjectRef(target));
      } else if (root instanceof JsonObject) {
        ((JsonObject) root).put(Utils.Pointers.unescape(prop), new JsonObjectRef(target));
      }

      return tree;
    } else {
      // undefined
      // System.out.println("applyRef: replace tree");
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
