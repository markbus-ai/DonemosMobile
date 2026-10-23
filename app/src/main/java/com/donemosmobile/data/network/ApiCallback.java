package com.donemosmobile.data.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Result of a repository call, delivered on the main thread. Every repository
 * shares this contract; {@link ApiCalls} decides which method fires.
 */
public interface ApiCallback<T> {

  void onSuccess(@Nullable T data);

  void onError(@NonNull ApiError.Type type, @Nullable String message);

  /**
   * Laravel 422 field errors: field -> first message. Falls back to
   * {@link #onError} so no screen stays silent.
   */
  default void onValidationError(@NonNull Map<String, String> fieldErrors) {
    onError(ApiError.Type.CLIENT, null);
  }
}
