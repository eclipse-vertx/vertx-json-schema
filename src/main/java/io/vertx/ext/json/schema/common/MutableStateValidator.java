package io.vertx.ext.json.schema.common;

public interface MutableStateValidator extends AsyncValidator, SyncValidator {

  /**
   * Returns the parent of this schema. This is required for sync state update
   *
   * @return
   */
  MutableStateValidator getParent();

  /**
   * Manually trigger the sync state update
   */
  void triggerUpdateIsSync();

}
