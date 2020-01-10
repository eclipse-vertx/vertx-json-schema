package io.vertx.ext.json.schema.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.draft7.Draft7SchemaParser;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.vertx.ext.json.schema.TestUtils.buildBaseUri;
import static io.vertx.ext.json.schema.TestUtils.loadJson;
import static io.vertx.ext.json.schema.asserts.MyAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class SchemaRouterRemoteRefTest {

  private HttpServer schemaServer;

  private static final Handler<RoutingContext> queryParamAuthMock = rc -> {
    if (rc.queryParam("francesco") == null || !"slinky".equals(rc.queryParam("francesco").get(0)))
      rc.fail(401);
    else
      rc.next();
  };

  private static final Handler<RoutingContext> headerAuthMock = BasicAuthHandler.create((jsonObject, handler) -> {
    if ("francesco".equals(jsonObject.getString("username")) && "slinky".equals(jsonObject.getString("password")))
      handler.handle(Future.succeededFuture(User.create(new JsonObject())));
    else
      handler.handle(Future.failedFuture("Not match"));
  });

  private void startSchemaServer(Vertx vertx, List<Handler<RoutingContext>> authHandlers, Handler<AsyncResult<Void>> completion) {
    Router r = Router.router(vertx);

    Route route = r.route("/*")
        .produces("application/json");
    authHandlers.forEach(route::handler);
    route.handler(StaticHandler.create("src/test/resources/remote").setCachingEnabled(true));

    schemaServer = vertx.createHttpServer(new HttpServerOptions().setPort(9000))
        .requestHandler(r)
        .listen(l -> completion.handle(Future.succeededFuture()));
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

  private void testValid(Vertx vertx, VertxTestContext context, SchemaRouterOptions options, List<Handler<RoutingContext>> authHandlers) throws IOException {
    Checkpoint check = context.checkpoint(2);

    URI mainSchemaURI = buildBaseUri("remote", "base.json");
    JsonObject mainSchemaUnparsed = loadJson(mainSchemaURI);
    SchemaRouter router = SchemaRouter.create(
        vertx,
        options
    );
    SchemaParserInternal parser = Draft7SchemaParser.create(router);

    startSchemaServer(vertx, authHandlers, v -> {
      Schema mainSchema = parser.parse(mainSchemaUnparsed, mainSchemaURI);

      mainSchema
          .validateAsync(new JsonObject().put("hello", 1))
          .setHandler(context.failing(e -> {
            context.verify(() -> {
              assertThat(e).isInstanceOf(ValidationException.class);
              assertThat(router)
                  .canResolveSchema(URIUtils.createJsonPointerFromURI(URI.create("http://localhost:9000/remote.json#hello")), mainSchema.getScope(), parser)
                  .hasXIdEqualsTo("hello_def");
            });
            check.flag();
          }));

      mainSchema
          .validateAsync(new JsonObject().put("hello", "a"))
          .setHandler(context.succeeding(v1 -> check.flag()));
    });
  }

  private void testInvalid(Vertx vertx, VertxTestContext context, SchemaRouterOptions options, List<Handler<RoutingContext>> authHandlers) throws IOException {
    URI mainSchemaURI = buildBaseUri("remote", "base.json");
    JsonObject mainSchemaUnparsed = loadJson(mainSchemaURI);
    SchemaRouter router = SchemaRouter.create(
        vertx,
        options
    );
    SchemaParserInternal parser = Draft7SchemaParser.create(router);

    startSchemaServer(vertx, authHandlers, v -> {
      Schema mainSchema = parser.parse(mainSchemaUnparsed, mainSchemaURI);

      mainSchema
          .validateAsync(new JsonObject().put("hello", "a"))
          .setHandler(context.failing(e -> {
            context.verify(() -> {
              assertThat(e)
                  .isInstanceOf(ValidationException.class)
                  .hasMessageStartingWith("Error while resolving reference");
              assertThat(router)
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
