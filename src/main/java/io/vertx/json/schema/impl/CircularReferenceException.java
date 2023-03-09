package io.vertx.json.schema.impl;


public class CircularReferenceException extends RuntimeException {
  public CircularReferenceException(String message) {
    super(message);
  }
}
