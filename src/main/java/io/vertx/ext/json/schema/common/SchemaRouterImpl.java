package io.vertx.ext.json.schema.common;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaRouterImpl implements SchemaRouter {

  private final Map<URI, RouterNode> absolutePaths;
  private final Map<URI, Object> rootJsons;
  private final HttpClient client;
  private final FileSystem fs;
  private final Map<URI, Future<Schema>> externalSchemasSolving;
  private final SchemaRouterOptions options;

  public SchemaRouterImpl(HttpClient client, FileSystem fs, SchemaRouterOptions options) {
    this.client = client;
    this.fs = fs;
    this.absolutePaths = new HashMap<>();
    this.rootJsons = new HashMap<>();
    this.externalSchemasSolving = new ConcurrentHashMap<>();
    this.options = options;
  }

  @Override
  public List<Schema> registeredSchemas() {
    return absolutePaths
        .values()
        .stream()
        .flatMap(RouterNode::flattened)
        .map(RouterNode::getSchema)
        .collect(Collectors.toList());
  }

  @Override
  public Schema resolveCachedSchema(JsonPointer refPointer, JsonPointer scope, final SchemaParser parser) {
    return resolveParentNode(refPointer, scope).flatMap(parentNode -> {
      Optional<RouterNode> resultNode = Optional.ofNullable((RouterNode) refPointer.query(parentNode, RouterNodeJsonPointerIterator.INSTANCE));
      if (resultNode.isPresent())
        return resultNode.map(RouterNode::getSchema);
      if (parentNode.getSchema() instanceof SchemaImpl) // Maybe the schema that we are searching was not parsed yet!
        return Optional.ofNullable(refPointer.queryJson(parentNode.getSchema().getJson()))
          .map(queryResult -> ((SchemaParserInternal)parser).parse(queryResult, URIUtils.replaceFragment(parentNode.getSchema().getScope().getURIWithoutFragment(), refPointer.toString())));
      return Optional.empty();
    }).orElse(null);
  }

  @Override
  public Future<Schema> resolveRef(final JsonPointer pointer, final JsonPointer scope, final SchemaParser schemaParser) {
    try {
      Schema cachedSchema = this.resolveCachedSchema(pointer, scope, schemaParser);
      if (cachedSchema == null) {
        return resolveExternalRef(pointer, scope, schemaParser);
      } else return Future.succeededFuture(cachedSchema);
    } catch (SchemaException e) {
      return Future.failedFuture(e);
    }
  }

  @Override
  public SchemaRouter addSchema(Schema schema, JsonPointer... aliasScope) {
    JsonPointer pointer = schema.getScope();
    if (!pointer.getURIWithoutFragment().isAbsolute()) {
      throw new IllegalStateException("Schema scope MUST be a pointer with an absolute URI. Actual: " + pointer.getURIWithoutFragment());
    }
    RouterNode parentNode = absolutePaths.computeIfAbsent(pointer.getURIWithoutFragment(), k -> new RouterNode());
    insertSchema(pointer, parentNode, schema);
    RouterNode insertedNode = (RouterNode) pointer.query(parentNode, RouterNodeJsonPointerIterator.INSTANCE);

    for (JsonPointer alias : aliasScope) {
      if (!alias.getURIWithoutFragment().isAbsolute()) {
        throw new IllegalStateException("Schema scope MUST be a pointer with an absolute URI. Actual: " + alias.getURIWithoutFragment());
      }

      insertRouterNode(alias, insertedNode);
    }

    return this;
  }

  @Override
  public SchemaRouter addSchemaWithScope(Schema schema, JsonPointer scope) {
    if (!scope.getURIWithoutFragment().isAbsolute()) {
      throw new IllegalStateException("Schema scope MUST be a pointer with an absolute URI. Actual: " + scope.getURIWithoutFragment());
    }
    RouterNode parentNode = absolutePaths.computeIfAbsent(scope.getURIWithoutFragment(), k -> new RouterNode());
    insertSchema(scope, parentNode, schema);
    return this;
  }

  @Override
  public SchemaRouter addSchemaAlias(Schema schema, String alias) {
    RouterNode parentNode = absolutePaths.get(schema.getScope().getURIWithoutFragment());
    if (parentNode == null) {
      throw new IllegalStateException("Schema parent node does not exists: " + schema.getScope().getURIWithoutFragment());
    }
    RouterNode schemaNode = (RouterNode) schema.getScope().query(parentNode, RouterNodeJsonPointerIterator.INSTANCE);
    if (schemaNode == null) {
      throw new IllegalStateException("Schema node does not exists: " + schema.getScope().toURI());
    }
    parentNode
      .getChilds()
      .put(alias, schemaNode);
    return this;
  }

  @Override
  public SchemaRouter addJson(URI uri, JsonObject object) {
    this.rootJsons.put(uri, object);
    return this;
  }

  /**
   * Deeply resolve all references of the provided {@code schema}
   *
   * @param schema
   * @return returns a succeeded future with same instance of provided schema, or a failed schema if something went wrong
   */
  public Future<Schema> solveAllSchemaReferences(Schema schema) {
    if (schema instanceof RefSchema) {
      return ((RefSchema) schema)
        .trySolveSchema()
        .compose(s -> (s != schema) ? solveAllSchemaReferences(s).map(schema) : Future.succeededFuture(schema));
    } else {
      // If not absolute, then there is nothing to resolve
      if (!schema.getScope().getURIWithoutFragment().isAbsolute()) {
        return Future.succeededFuture(schema);
      }
      RouterNode node = absolutePaths.get(schema.getScope().getURIWithoutFragment());
      node = (RouterNode) schema.getScope().query(node, RouterNodeJsonPointerIterator.INSTANCE);
      return CompositeFuture.all(
        node
          .reverseFlattened()
          .collect(Collectors.toList())// Must create a collection to avoid ConcurrentModificationException
          .stream()
          .map(RouterNode::getSchema)
          .filter(Objects::nonNull)
          .filter(s -> s instanceof RefSchema)
          .map(s -> (RefSchema) s)
          .map(RefSchema::trySolveSchema)
          .collect(Collectors.toList())
      ).map(schema);
    }
  }

  // The idea is to traverse from base to actual scope all tree and find aliases
  private Stream<URI> getScopeParentAliases(JsonPointer scope) {
    Stream.Builder<URI> uriStreamBuilder = Stream.builder();
    RouterNode startingNode = absolutePaths.get(scope.getURIWithoutFragment());
    scope.tracedQuery(startingNode, RouterNodeJsonPointerIterator.INSTANCE)
      .forEach((node) -> absolutePaths.forEach((uri, n) -> {
        if (n == node) uriStreamBuilder.accept(uri);
      }));
    return uriStreamBuilder.build();
  }

  private Optional<RouterNode> resolveParentNode(JsonPointer refPointer, JsonPointer scope) {
    URI refURI = refPointer.getURIWithoutFragment();
    if (!refURI.isAbsolute()) {
      if (refURI.getPath() != null && !refURI.getPath().isEmpty()) {
        // Path pointer
        return Stream.concat(
          getScopeParentAliases(scope).map(e -> URIUtils.resolvePath(e, refURI.getPath())),
          Stream.of(
            getResourceAbsoluteURIFromClasspath(refURI),
            refURI
          )
        )
          .map(absolutePaths::get)
          .filter(Objects::nonNull)
          .findFirst();
      } else {
        // Fragment pointer, fallback to scope
        return Optional.ofNullable(absolutePaths.get(scope.getURIWithoutFragment()));
      }
    } else {
      // Absolute pointer
      return Optional.ofNullable(absolutePaths.get(refURI));
    }
  }

  private Future<String> solveRemoteRef(final URI ref) {
    String uri = ref.toString();
    if (!options.getAuthQueryParams().isEmpty()) {
      QueryStringEncoder encoder = new QueryStringEncoder(uri);
      options.getAuthQueryParams().forEach(encoder::addParam);
      uri = encoder.toString();
    }

    RequestOptions reqOptions = new RequestOptions()
      .setMethod(HttpMethod.GET)
      .setAbsoluteURI(uri)
      .setFollowRedirects(true)
      .addHeader(HttpHeaders.ACCEPT.toString(), "application/json, application/schema+json");

    options.getAuthHeaders().forEach(reqOptions::addHeader);

    return client.get(reqOptions).compose(res -> {
      if (res.statusCode() == 200) {
        return res.body().map(Buffer::toString);
      }
      return Future.failedFuture(new IllegalStateException("Wrong status " + res.statusCode() + " " + res.statusMessage() + " received while resolving remote ref"));
    });
  }

  private Future<String> solveLocalRef(final URI ref) {
    Promise<String> promise = Promise.promise();
    String filePath = ("jar".equals(ref.getScheme())) ? ref.getSchemeSpecificPart().split("!")[1].substring(1) : ref.getPath();
    fs.readFile(filePath, res -> {
      if (res.succeeded()) {
        promise.complete(res.result().toString());
      } else {
        promise.fail(res.cause());
      }
    });
    return promise.future();
  }

  private Future<Schema> resolveExternalRef(final JsonPointer pointer, final JsonPointer scope, final SchemaParser schemaParser) {
    URI refURI = pointer.getURIWithoutFragment();
    return externalSchemasSolving.computeIfAbsent(refURI, (r) -> {
      Stream<URI> candidatesURIs;
      if (refURI.isAbsolute()) // $ref uri is absolute, just solve it
        candidatesURIs = Stream.of(refURI);
      else // $ref is relative, so it should resolve all aliases of scope and then relativize
        candidatesURIs = Stream.concat(
          getScopeParentAliases(scope)
            .map(u -> URIUtils.resolvePath(u, refURI.getPath()))
            .filter(u -> URIUtils.isRemoteURI(u) || URIUtils.isLocalURI(u)) // Remove aliases not resolvable
            .sorted((u1, u2) -> (URIUtils.isLocalURI(u1) && !URIUtils.isLocalURI(u2)) ? 1 : (u1.equals(u2)) ? 0 : -1), // Try to solve local refs before
          Stream.of(
            getResourceAbsoluteURIFromClasspath(refURI) // Last hope: try to solve from class loader
          )
        );
      URI uriToSolve = candidatesURIs
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(refURI); // REALLY Last hope: try to solve as is from file system

      if (rootJsons.containsKey(uriToSolve)) { // Cached!
        Object realLocation = pointer.queryJson(rootJsons.get(uriToSolve));
        ((SchemaParserInternal)schemaParser).parse(
          realLocation,
          JsonPointer.fromURI(URIUtils.replaceFragment(uriToSolve, pointer.toString()))
        );
        return Future.succeededFuture(resolveCachedSchema(pointer, scope, schemaParser));
      }
      return
        ((URIUtils.isRemoteURI(uriToSolve)) ? solveRemoteRef(uriToSolve) : solveLocalRef(uriToSolve))
          .map(s -> {
            Object root = Json.decodeValue(s.trim());
            this.rootJsons.put(uriToSolve, root);
            Object realSchema = pointer.queryJson(root);
            ((SchemaParserInternal)schemaParser).parse(
              realSchema,
              JsonPointer.fromURI(URIUtils.replaceFragment(uriToSolve, pointer.toString()))
            );
            return resolveCachedSchema(pointer, scope, schemaParser);
          });
    });
  }

  private URI getResourceAbsoluteURIFromClasspath(URI u) {
    try {
      return getClassLoader().getResource(u.toString()).toURI();
    } catch (NullPointerException | URISyntaxException e) {
      return null;
    }
  }

  private ClassLoader getClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = getClass().getClassLoader();
    }
    // when running on substratevm (graal) the access to class loaders
    // is very limited and might be only available from compile time
    // known classes. (Object is always known, so we do a final attempt
    // to get it here).
    if (cl == null) {
      cl = Object.class.getClassLoader();
    }
    return cl;
  }

  public void insertSchema(JsonPointer pointer, RouterNode initialNode, Schema schema) {
    if (pointer.isRootPointer())
      initialNode.setSchema(schema);
    else
      pointer.write(
        initialNode,
        RouterNodeJsonPointerIterator.INSTANCE,
        schema,
        true
      );
  }

  public void insertRouterNode(JsonPointer pointer, RouterNode nodeToWrite) {
    if (pointer.isRootPointer())
      absolutePaths.put(pointer.getURIWithoutFragment(), nodeToWrite);
    else
      pointer.write(
        absolutePaths.computeIfAbsent(pointer.getURIWithoutFragment(), k -> new RouterNode()),
        RouterNodeJsonPointerIterator.INSTANCE,
        nodeToWrite,
        true
      );
  }
}
