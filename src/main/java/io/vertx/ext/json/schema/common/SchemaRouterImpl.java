package io.vertx.ext.json.schema.common;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
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
  public SchemaRouter addSchema(Schema schema) {
    URI schemaScopeWithoutFragment = schema.getScope().getURIWithoutFragment();
    absolutePaths.putIfAbsent(schemaScopeWithoutFragment, new RouterNode());

    RouterNode parentNode = absolutePaths.get(schemaScopeWithoutFragment);
    if (schema.getScope().isRootPointer()) {
      parentNode.setSchema(schema);
    } else
      schema.getScope().write(
              parentNode,
              RouterNodeJsonPointerIterator.INSTANCE,
              schema,
              true
      );

    // Handle $id keyword
    if (schema instanceof SchemaImpl && ((SchemaImpl) schema).getJson().containsKey("$id")) {
      RouterNode wroteNode = (RouterNode) schema.getScope().query(parentNode, RouterNodeJsonPointerIterator.INSTANCE);
      try {
        String unparsedId = ((SchemaImpl) schema).getJson().getString("$id");
        URI id = URI.create(unparsedId);
        JsonPointer idPointer = URIUtils.createJsonPointerFromURI(id);
        // Create parent node aliases if needed
        if (id.isAbsolute()) { // Absolute id
          absolutePaths.putIfAbsent(URIUtils.removeFragment(id), wroteNode); // id and inferredScope can match!
        } else if (id.getPath() != null && !id.getPath().isEmpty()) {
          // If a path is relative you should solve the path/paths. The paths will be solved against aliases of base node of inferred scope
          List<URI> paths = absolutePaths
              .entrySet()
              .stream()
              .filter(e -> e.getValue().equals(parentNode))
              .map(e -> URIUtils.resolvePath(e.getKey(), id.getPath()))
              .collect(Collectors.toList());
          paths.forEach(u -> absolutePaths.put(u, wroteNode));
        }
        // Write the alias down the tree
        if (!idPointer.isRootPointer())
          idPointer.write(parentNode, RouterNodeJsonPointerIterator.INSTANCE, wroteNode, true);
      } catch (IllegalArgumentException e) {
        throw new SchemaException(schema, "$id keyword should be a valid URI", e);
      }
    }
    return this;
  }

  @Override
  public SchemaRouter addJson(URI uri, JsonObject object) {
    this.rootJsons.put(uri, object);
    return this;
  }

  // The idea is to traverse from base to actual scope all tree and find aliases
  private Stream<URI> getScopeParentAliases(JsonPointer scope) {
    Stream.Builder<URI> uriStreamBuilder = Stream.builder();
    RouterNode startingNode = absolutePaths.get(scope.getURIWithoutFragment());
    scope.tracedQuery(startingNode, RouterNodeJsonPointerIterator.INSTANCE)
            .forEach((node) -> absolutePaths.forEach((uri, n) -> { if (n == node) uriStreamBuilder.accept(uri); }));
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
    Promise<String> promise = Promise.promise();
    String uri = ref.toString();
    if (!options.getAuthQueryParams().isEmpty()) {
      QueryStringEncoder encoder = new QueryStringEncoder(uri);
      options.getAuthQueryParams().forEach(encoder::addParam);
      uri = encoder.toString();
    }
    HttpClientRequest req = client.getAbs(uri, ar -> {
      if (ar.failed()) {
        promise.fail(ar.cause());
        return;
      }
      ar.result().exceptionHandler(promise::fail);
      if (ar.result().statusCode() == 200) {
        ar.result().bodyHandler(buf -> {
          promise.complete(buf.toString());
        });
      } else {
        promise.fail(new IllegalStateException("Wrong status code " + ar.result().statusCode() + " " + ar.result().statusMessage() + " received while resolving remote ref"));
      }
    })
      .setFollowRedirects(true)
      .putHeader(HttpHeaders.ACCEPT.toString(), "application/json, application/schema+json");
    options.getAuthHeaders().forEach(req::putHeader);
    req.end();
    return promise.future();
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

  @Override
  public Future<Schema> solveAllSchemaReferences(Schema schema) {
    if (schema instanceof RefSchema) {
      return ((RefSchema) schema)
          .trySolveSchema()
          .compose(s -> (s != schema) ? solveAllSchemaReferences(s).map(schema) : Future.succeededFuture(schema));
    } else {
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
              .map(s -> (RefSchema)s)
              .map(RefSchema::trySolveSchema)
              .collect(Collectors.toList())
      ).map(schema);
    }
  }

}
