package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the options to resolve external schemas. You can configure auth options to access to external services
 *
 */
@DataObject(generateConverter = true)
public class SchemaRouterOptions {

  private Map<String, String> authQueryParams;
  private Map<String, String> authHeaders;

  public SchemaRouterOptions() {
    authHeaders = new HashMap<>();
    authQueryParams = new HashMap<>();
  }

  public SchemaRouterOptions(JsonObject obj) {
    SchemaRouterOptionsConverter.fromJson(obj, this);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    SchemaRouterOptionsConverter.toJson(this, obj);
    return obj;
  }

  /**
   * Put an header to authenticate requests while loading an external schema
   *
   * @param headerName
   * @param headerValue
   * @return
   */
  @Fluent
  public SchemaRouterOptions putAuthHeader(String headerName, String headerValue) {
    authHeaders.put(headerName, headerValue);
    return this;
  }

  /**
   * Put a query parameter to authenticate requests while loading an external schema
   *
   * @param queryParamName
   * @param queryParamValue
   * @return
   */
  @Fluent
  public SchemaRouterOptions putAuthQueryParam(String queryParamName, String queryParamValue) {
    authQueryParams.put(queryParamName, queryParamValue);
    return this;
  }

  public Map<String, String> getAuthQueryParams() {
    return authQueryParams;
  }

  public Map<String, String> getAuthHeaders() {
    return authHeaders;
  }
}
