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

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaRouterImpl implements SchemaRouter {

  private final Vertx vertx;
  private final Map<URI, RouterNode> absolutePaths;
  private final Map<URI, Object> rootJsons;
  private final HttpClient client;
  private final FileSystem fs;
  private final Map<URI, Future<Schema>> externalSchemasSolving;
  private final SchemaRouterOptions options;

  private final String cacheDir;

  private static String resolveCanonical(Vertx vertx, String path) {
    try {
      File canonicalFile = ((VertxInternal) vertx).resolveFile(path).getCanonicalFile();
      return slashify(canonicalFile.getPath(), canonicalFile.isDirectory());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public SchemaRouterImpl(Vertx vertx, HttpClient client, FileSystem fs, SchemaRouterOptions options) {
    this.vertx = vertx;
    this.client = client;
    this.fs = fs;
    this.absolutePaths = new HashMap<>();
    this.rootJsons = new HashMap<>();
    this.externalSchemasSolving = new ConcurrentHashMap<>();
    this.options = options;
    this.cacheDir = resolveCanonical(vertx, "");
  }

  @Override
  public List<Schema> registeredSchemas() {
    return absolutePaths
      .values()
      .stream()
      .flatMap(RouterNode::flattened)
      .map(RouterNode::getSchema)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public Schema resolveCachedSchema(JsonPointer refPointer, JsonPointer scope, final SchemaParser parser) {
    // Let's try first searching in already parsed cached schemas
    return resolveParentNode(refPointer, scope)
      .flatMap(parentNode -> {
        Optional<RouterNode> resultNode = Optional.ofNullable((RouterNode) refPointer.query(parentNode, RouterNodeJsonPointerIterator.INSTANCE));
        if (resultNode.isPresent())
          return resultNode.map(RouterNode::getSchema);
        if (parentNode.getSchema() instanceof SchemaImpl) // Maybe the schema that we are searching was not parsed yet!
          return Optional.ofNullable(refPointer.queryJson(parentNode.getSchema().getJson()))
            .map(queryResult -> ((SchemaParserInternal) parser).parse(queryResult, URIUtils.replaceFragment(parentNode.getSchema().getScope().getURIWithoutFragment(), refPointer.toString())));
        return Optional.empty();
      })
      // Let's try from the rootJson, user could be requesting a json added but not parsed
      .orElseGet(() -> resolveAbsoluteUriAlternatives(refPointer, scope)
        .filter(rootJsons::containsKey)
        .map(uriToSolve -> {
          Object realLocation = refPointer.queryJson(rootJsons.get(uriToSolve));
          if (realLocation == null) {
            return null;
          }
          return ((SchemaParserInternal) parser).parse(
            realLocation,
            JsonPointer.fromURI(URIUtils.replaceFragment(uriToSolve, refPointer.toString()))
          );
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null)
      );
  }

  @Override
  public void resolveRef(JsonPointer pointer, JsonPointer scope, SchemaParser schemaParser, Handler<AsyncResult<Schema>> handler) {
    Future<Schema> fut = resolveRef(pointer, scope, schemaParser);
    if (handler != null) {
      fut.onComplete(handler);
    }
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
    URIUtils.requireAbsoluteUri(scope.getURIWithoutFragment(), "schema scope");
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
    URIUtils.requireAbsoluteUri(uri);
    this.rootJsons.put(uri, object);
    return this;
  }

  // Very very expensive method
  public Future<Void> resolveAllSchemas() {
    return CompositeFuture
      .all(this.registeredSchemas().stream().map(this::solveAllSchemaReferences).collect(Collectors.toList()))
      .mapEmpty();
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
      if (schema.getScope().getURIWithoutFragment() == null || !schema.getScope().getURIWithoutFragment().isAbsolute()) {
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
    // If this is the first external ref solved by this schema router, then the startingNode could not be there
    if (startingNode == null) {
      return Stream.of(scope.getURIWithoutFragment());
    }
    scope.tracedQuery(startingNode, RouterNodeJsonPointerIterator.INSTANCE)
      .forEach((node) -> absolutePaths.forEach((uri, n) -> {
        if (n == node) uriStreamBuilder.accept(uri);
      }));
    return uriStreamBuilder.build();
  }

  private Stream<URI> resolveAbsoluteUriAlternatives(JsonPointer refPointer, JsonPointer scope) {
    URI refURI = refPointer.getURIWithoutFragment();
    if (!refURI.isAbsolute()) {
      if (refURI.getPath() != null && !refURI.getPath().isEmpty()) {
        // Path pointer
        return Stream.concat(
          getScopeParentAliases(scope)
            .map(e -> URIUtils.resolvePath(e, refURI.getPath())),
          Stream.of(
            getResourceAbsoluteURI(refURI),
            refURI
          )
        ).filter(Objects::nonNull);
      } else {
        // Fragment pointer, fallback to scope
        return Stream.of(scope.getURIWithoutFragment());
      }
    } else {
      // Absolute pointer
      return Stream.of(refURI);
    }
  }

  private Optional<RouterNode> resolveParentNode(JsonPointer refPointer, JsonPointer scope) {
    return resolveAbsoluteUriAlternatives(refPointer, scope)
      .map(absolutePaths::get)
      .filter(Objects::nonNull)
      .findFirst();
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

    return client.request(reqOptions)
      .compose(req -> req.send()
        .compose(resp -> {
          int statusCode = resp.statusCode();
          if (statusCode < 200 || statusCode > 299) {
            return Future.failedFuture(new IllegalStateException("Wrong status " + statusCode + " " + resp.statusMessage() + " received while resolving remote ref"));
          } else {
            return resp
              .body()
              .map(Buffer::toString);
          }
        }));
  }

  private String relativizePathToBase(String filePath) {
    return
      filePath.startsWith(cacheDir) ?
        filePath.substring(cacheDir.length()) :
        filePath;
  }

  private Future<String> solveLocalRef(final URI ref) {
    Promise<String> promise = Promise.promise();
    fs.readFile(relativizePathToBase(ref.getPath()), res -> {
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
      if (refURI.isAbsolute()) { // $ref uri is absolute, just solve it
        candidatesURIs = Stream.of(refURI);
      } else { // $ref is relative, so it should resolve all aliases of scope and then relativize
        candidatesURIs = Stream.concat(
          getScopeParentAliases(scope)
            .map(u -> URIUtils.resolvePath(u, refURI.getPath()))
            .filter(u -> URIUtils.isRemoteURI(u) || URIUtils.isLocalURI(u)) // Remove aliases not resolvable
            .sorted((u1, u2) -> (URIUtils.isLocalURI(u1) && !URIUtils.isLocalURI(u2)) ? 1 : (u1.equals(u2)) ? 0 : -1), // Try to solve local refs before
          Stream.of(
            getResourceAbsoluteURI(refURI) // Last hope: try to solve from class loader
          )
        );
      }
      URI uriToSolve = candidatesURIs
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(refURI); // REALLY Last hope: try to solve as is from file system

      return
        ((URIUtils.isRemoteURI(uriToSolve)) ? solveRemoteRef(uriToSolve) : solveLocalRef(uriToSolve))
          .map(s -> {
            Object root = Json.decodeValue(s.trim());
            this.rootJsons.put(uriToSolve, root);
            Object realSchema = pointer.queryJson(root);
            ((SchemaParserInternal) schemaParser).parse(
              realSchema,
              JsonPointer.fromURI(URIUtils.replaceFragment(uriToSolve, pointer.toString()))
            );
            return resolveCachedSchema(pointer, scope, schemaParser);
          });
    });
  }

  private URI getResourceAbsoluteURI(URI source) {
    String path = source.getPath();
    File resolved = ((VertxInternal) vertx).resolveFile(path);
    URI uri = null;
    if (resolved != null) {
      if (resolved.exists()) {
        try {
          resolved = resolved.getCanonicalFile();
          uri = new URI("file://" + slashify(resolved.getPath(), resolved.isDirectory()));
        } catch (URISyntaxException | IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return uri;
  }

  private static String slashify(String path, boolean isDirectory) {
    String p = path;
    if (File.separatorChar != '/')
      p = p.replace(File.separatorChar, '/');
    if (!p.startsWith("/"))
      p = "/" + p;
    if (!p.endsWith("/") && isDirectory)
      p = p + "/";
    return p;
  }

  public void insertSchema(JsonPointer pointer, RouterNode initialNode, Schema schema) {
    if (pointer.isRootPointer())
      initialNode.setSchema((SchemaInternal) schema);
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
