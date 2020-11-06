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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static io.vertx.json.schema.TestUtils.loadJson;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class SchemaRouterRemoteRefTest {

  private HttpServer schemaServer;

  private static final Predicate<HttpServerRequest> queryParamAuthMock =
    httpRequest -> httpRequest.getParam("francesco") != null && "slinky".equals(httpRequest.getParam("francesco"));

  private static final Predicate<HttpServerRequest> headerAuthMock = httpServerRequest -> {
    final String authHandler = httpServerRequest.getHeader("Authorization");
    if (authHandler == null || !authHandler.contains("Basic ")) {
      return false;
    }

    String parseAuthorization = authHandler.substring("Basic ".length());

    final String suser;
    final String spass;

    try {
      // decode the payload
      String decoded = new String(Base64.getDecoder().decode(parseAuthorization.trim()), StandardCharsets.UTF_8);

      int colonIdx = decoded.indexOf(":");
      if (colonIdx != -1) {
        suser = decoded.substring(0, colonIdx);
        spass = decoded.substring(colonIdx + 1);
      } else {
        suser = decoded;
        spass = null;
      }
    } catch (RuntimeException e) {
      // IllegalArgumentException includes PatternSyntaxException
      return false;
    }

    return "francesco".equals(suser) && "slinky".equals(spass);
  };

  private Future<Void> startSchemaServer(Vertx vertx, List<Predicate<HttpServerRequest>> authPredicates) {
    Predicate<HttpServerRequest> p = authPredicates.stream().reduce(h -> true, Predicate::and);

    schemaServer = vertx.createHttpServer(new HttpServerOptions().setPort(9000))
      .requestHandler(req -> {
        String path = req.path();

        if (!p.test(req)) {
          req.response()
            .setStatusCode(401)
            .end();
        } else {
          req.response()
            .putHeader("Content-type", "application/json")
            .sendFile(Paths.get("src/test/resources/remote", path).toString());
        }
      });

    return schemaServer.listen().mapEmpty();
  }

  private void stopSchemaServer(Handler<AsyncResult<Void>> completion) {
    if (schemaServer != null) {
      try {
        schemaServer.close((asyncResult) -> {
          completion.handle(Future.succeededFuture());
        });
      } catch (IllegalStateException e) { // Server is already open
        completion.handle(Future.succeededFuture());
      }
    }
  }

  @AfterEach
  public void tearDown(VertxTestContext testContext) throws Exception {
    stopSchemaServer(testContext.completing());
  }

  private void testValid(Vertx vertx, VertxTestContext context, SchemaRouterOptions options, List<Predicate<HttpServerRequest>> authHandlers) throws IOException {
    Checkpoint check = context.checkpoint(2);

    URI mainSchemaURI = buildBaseUri("remote", "base.json");
    JsonObject mainSchemaUnparsed = loadJson(mainSchemaURI);
    SchemaRouter router = SchemaRouter.create(
      vertx,
      options
    );
    SchemaParserInternal parser = Draft7SchemaParser.create(router);

    context.assertComplete(startSchemaServer(vertx, authHandlers)).onSuccess(v -> {
      Schema mainSchema = parser.parse(mainSchemaUnparsed, mainSchemaURI);

      mainSchema
        .validateAsync(new JsonObject().put("hello", 1))
        .onComplete(context.failing(e -> {
          context.verify(() -> {
            assertThat(e).isInstanceOf(ValidationException.class);
            MyAssertions.assertThat(router)
              .canResolveSchema(URIUtils.createJsonPointerFromURI(URI.create("http://localhost:9000/remote.json#hello")), mainSchema.getScope(), parser)
              .hasXIdEqualsTo("hello_def");
          });
          check.flag();
        }));

      mainSchema
        .validateAsync(new JsonObject().put("hello", "a"))
        .onComplete(context.succeeding(v1 -> check.flag()));
    });
  }

  private void testInvalid(Vertx vertx, VertxTestContext context, SchemaRouterOptions options, List<Predicate<HttpServerRequest>> authHandlers) throws IOException {
    URI mainSchemaURI = buildBaseUri("remote", "base.json");
    JsonObject mainSchemaUnparsed = loadJson(mainSchemaURI);
    SchemaRouter router = SchemaRouter.create(
      vertx,
      options
    );
    SchemaParserInternal parser = Draft7SchemaParser.create(router);

    context.assertComplete(startSchemaServer(vertx, authHandlers)).onSuccess(v -> {
      Schema mainSchema = parser.parse(mainSchemaUnparsed, mainSchemaURI);

      mainSchema
        .validateAsync(new JsonObject().put("hello", "a"))
        .onComplete(context.failing(e -> {
          context.verify(() -> {
            assertThat(e)
              .isInstanceOf(ValidationException.class)
              .hasMessageStartingWith("Error while resolving reference");
            MyAssertions.assertThat(router)
              .cannotResolveSchema(URIUtils.createJsonPointerFromURI(URI.create("http://localhost:9000/remote.json#hello")), mainSchema.getScope(), parser);
          });
          context.completeNow();
        }));
    });
  }

  @Test
  public void queryAuthRef(Vertx vertx, VertxTestContext context) throws IOException {
    testValid(
      vertx,
      context,
      new SchemaRouterOptions().putAuthQueryParam("francesco", "slinky"),
      Collections.singletonList(queryParamAuthMock)
    );
  }

  @Test
  public void queryAuthRefInvalid(Vertx vertx, VertxTestContext context) throws IOException {
    testInvalid(
      vertx,
      context,
      new SchemaRouterOptions().putAuthQueryParam("francesco", "bla"),
      Collections.singletonList(queryParamAuthMock)
    );
  }

  @Test
  public void headerAuthRef(Vertx vertx, VertxTestContext context) throws IOException {
    testValid(
      vertx,
      context,
      new SchemaRouterOptions().putAuthHeader("Authorization", "Basic ZnJhbmNlc2NvOnNsaW5reQ=="),
      Collections.singletonList(headerAuthMock)
    );
  }

  @Test
  public void headerAuthRefInvalid(Vertx vertx, VertxTestContext context) throws IOException {
    testInvalid(
      vertx,
      context,
      new SchemaRouterOptions().putAuthHeader("Authorization", "Basic ZnJhbmNlc2NvOmJsYQ=="),
      Collections.singletonList(headerAuthMock)
    );
  }

  @Test
  public void bothAuthRef(Vertx vertx, VertxTestContext context) throws IOException {
    testValid(
      vertx,
      context,
      new SchemaRouterOptions()
        .putAuthQueryParam("francesco", "slinky")
        .putAuthHeader("Authorization", "Basic ZnJhbmNlc2NvOnNsaW5reQ=="),
      Arrays.asList(queryParamAuthMock, headerAuthMock)
    );
  }

}
