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
package io.vertx.json.schema;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(VertxExtension.class)
public class CBORTest {

  private static final Base64.Encoder B64ENC = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64DEC = Base64.getUrlDecoder();
  private static final CBORFactory FACTORY = new CBORFactory();

  private static Map<String, ?> CBOR;

  @BeforeAll
  public static void init() throws IOException {
    // This is a simple webauthn CBOR dump
    // {
    //   fmt: none
    //   authData: {}
    //   attStmt: "very long string..."
    // }
    String data = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVjEfxV8VVBPmz66RLzscHpg5yjRhO28Y_fPwYO5AVwzBEJBAAAAAwAAAAAAAAAAAAAAAAAAAAAAQEPjBz9F6ttAijOS1t6gbfYLub3TmCzWXN4wnIMsi53EWr-SL3e09XWr93lqyOwk_B5s1P8gGCa5o2uIp_DhS9ylAQIDJiABIVggN_D3u-03a0GzONOHfaML881QZtOCc5oTNRB2wlyqUEUiWCD3878XoO_bIJf0mEPDILODFhVmkc4QeR6hOIDvwvXzYQ";

    CBOR = parse(FACTORY.createParser(B64DEC.decode(data)));
  }


  @Test
  public void schemaAndValidationUsingCBOR(Vertx vertx) {

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(router);

    final Schema schema = parser.parse(
      new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("fmt").add("authData").add("attStmt")));

    // OK
    assertThatCode(() -> schema.validateSync(CBOR))
      .doesNotThrowAnyException();
  }

  @Test
  public void schemaAndValidationUsingCBORInvalid(Vertx vertx) {

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser parser = SchemaParser.createDraft7SchemaParser(router);

    final Schema schema = parser.parse(
      new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("fmt2")));

    // OK
    assertThatThrownBy(() -> schema.validateSync(CBOR))
      .isInstanceOf(ValidationException.class)
      .hasMessageContaining("provided object should contain property fmt2");
  }

  public static <T> T parse(JsonParser parser) throws DecodeException {
    try {
      if (parser.currentToken() == null) {
        parser.nextToken();
      }
      return (T) parseAny(parser);
    } catch (IOException e) {
      throw new DecodeException(e.getMessage(), e);
    }
  }

  private static Object parseAny(JsonParser parser) throws IOException, DecodeException {
    switch (parser.getCurrentTokenId()) {
      case JsonTokenId.ID_START_OBJECT:
        return parseObject(parser);
      case JsonTokenId.ID_START_ARRAY:
        return parseArray(parser);
      case JsonTokenId.ID_EMBEDDED_OBJECT:
        return B64ENC.encodeToString(parser.getBinaryValue(Base64Variants.MODIFIED_FOR_URL));
      case JsonTokenId.ID_STRING:
        return parser.getText();
      case JsonTokenId.ID_NUMBER_FLOAT:
      case JsonTokenId.ID_NUMBER_INT:
        return parser.getNumberValue();
      case JsonTokenId.ID_TRUE:
        return Boolean.TRUE;
      case JsonTokenId.ID_FALSE:
        return Boolean.FALSE;
      case JsonTokenId.ID_NULL:
        return null;
      default:
        throw new DecodeException("Unexpected token"/*, parser.getCurrentLocation()*/);
    }
  }

  private static Map<String, Object> parseObject(JsonParser parser) throws IOException {
    String key1 = parser.nextFieldName();
    if (key1 == null) {
      return new LinkedHashMap<>(2);
    }
    parser.nextToken();
    Object value1 = parseAny(parser);
    String key2 = parser.nextFieldName();
    if (key2 == null) {
      LinkedHashMap<String, Object> obj = new LinkedHashMap<>(2);
      obj.put(key1, value1);
      return obj;
    }
    parser.nextToken();
    Object value2 = parseAny(parser);
    String key = parser.nextFieldName();
    if (key == null) {
      LinkedHashMap<String, Object> obj = new LinkedHashMap<>(2);
      obj.put(key1, value1);
      obj.put(key2, value2);
      return obj;
    }
    // General case
    LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
    obj.put(key1, value1);
    obj.put(key2, value2);
    do {
      parser.nextToken();
      Object value = parseAny(parser);
      obj.put(key, value);
      key = parser.nextFieldName();
    } while (key != null);
    return obj;
  }

  private static List<Object> parseArray(JsonParser parser) throws IOException {
    List<Object> array = new ArrayList<>();
    while (true) {
      parser.nextToken();
      int tokenId = parser.getCurrentTokenId();
      if (tokenId == JsonTokenId.ID_FIELD_NAME) {
        throw new UnsupportedOperationException();
      } else if (tokenId == JsonTokenId.ID_END_ARRAY) {
        return array;
      }
      Object value = parseAny(parser);
      array.add(value);
    }
  }
}
