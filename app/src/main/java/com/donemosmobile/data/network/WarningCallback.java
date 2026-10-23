package com.donemosmobile.data.network;

import androidx.annotation.NonNull;

/**
 * {@link ApiCallback} for writes whose business rules answer a 409 envelope
 * the user can act on (for example, force the operation with an audited
 * authorization).
 */
public interface WarningCallback<T, W> extends ApiCallback<T> {

  void onWarning(@NonNull W warning);
}
