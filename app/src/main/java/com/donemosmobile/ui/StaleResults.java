package com.donemosmobile.ui;

import androidx.annotation.Nullable;

/**
 * Decides whether an asynchronous result still belongs to what the screen
 * shows. A result is bound to the patient (DNI) it was requested for; when the
 * screen is gone or the current selection is another patient, it is dropped.
 */
public final class StaleResults {

  private StaleResults() {}

  /**
   * @param screenGone true when the activity is finishing or destroyed
   * @param requestedDni DNI the request was sent for
   * @param currentDni DNI the screen currently holds (latest lookup or selection)
   * @return true when the result must be ignored
   */
  public static boolean isStale(boolean screenGone, @Nullable String requestedDni,
      @Nullable String currentDni) {
    return screenGone || requestedDni == null || !requestedDni.equals(currentDni);
  }
}
