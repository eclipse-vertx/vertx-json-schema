package io.vertx.json.schema.internal;

public class SchemaValidatorState {

  public int currentIndex;
  public boolean stop;

  public SchemaValidatorState(int currentIndex, boolean stop) {
    this.currentIndex =  currentIndex;
    this.stop = stop;
  }

}
