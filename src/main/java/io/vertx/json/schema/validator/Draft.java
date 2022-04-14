package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public enum Draft {
  DRAFT4,
  DRAFT7,
  DRAFT201909,
  DRAFT202012;

  public static Draft from(String string) {
    switch (string) {
      case "4":
        return DRAFT4;
      case "7":
        return DRAFT7;
      case "8":
      case "2019-09":
        return DRAFT201909;
      case "2020-12":
        return DRAFT202012;
      default:
        throw new IllegalArgumentException("Invalid draft: " + string);
    }
  }
}
