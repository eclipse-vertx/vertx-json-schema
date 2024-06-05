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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class CBORTest {

  private static final Base64.Decoder B64DEC = Base64.getUrlDecoder();

  private static Map<String, ?> cbor;

  @BeforeAll
  public static void init() throws IOException {
    // This is a simple webauthn CBOR dump
    // {
    //   fmt: none
    //   authData: {}
    //   attStmt: "very long string..."
    // }
    String data = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVjEfxV8VVBPmz66RLzscHpg5yjRhO28Y_fPwYO5AVwzBEJBAAAAAwAAAAAAAAAAAAAAAAAAAAAAQEPjBz9F6ttAijOS1t6gbfYLub3TmCzWXN4wnIMsi53EWr-SL3e09XWr93lqyOwk_B5s1P8gGCa5o2uIp_DhS9ylAQIDJiABIVggN_D3u-03a0GzONOHfaML881QZtOCc5oTNRB2wlyqUEUiWCD3878XoO_bIJf0mEPDILODFhVmkc4QeR6hOIDvwvXzYQ";

    try (CBOR parser = new CBOR(B64DEC.decode(data))) {
      // force the decoded message to be a plain map
      cbor = ((JsonObject) parser.read()).getMap();
    }
  }


  @Test
  public void schemaAndValidationUsingCBOR() {
    final Validator validator = Validator.create(
      JsonSchema.of(
        new JsonObject()
          .put("type", "object")
          .put("required", new JsonArray()
            .add("fmt").add("authData").add("attStmt"))),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT7)
        .setOutputFormat(OutputFormat.Basic));

    final OutputUnit res = validator.validate(cbor);

    assertThat(res.getValid()).isTrue();
    assertThat(res.getErrors()).isNull();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.NONE);
  }

  @Test
  public void schemaAndValidationUsingCBORInvalid() {
    final Validator validator = Validator.create(
      JsonSchema.of(
        new JsonObject()
          .put("type", "object")
          .put("required", new JsonArray()
            .add("fmt2"))),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT7)
        .setOutputFormat(OutputFormat.Basic));

    final OutputUnit res = validator.validate(cbor);

    assertThat(res.getValid()).isFalse();
    assertThat(res.getErrors()).isNotEmpty();
    assertThat(res.getErrorType()).isEqualByComparingTo(OutputErrorType.MISSING_VALUE);

    try {
      res.checkValidity();
      fail("Should have thrown an exception");
    } catch (JsonSchemaValidationException e) {
      assertThat(e.getMessage()).contains("Instance does not have required property \"fmt2\"");
    }
  }
}
