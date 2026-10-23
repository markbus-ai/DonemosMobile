package com.donemosmobile.data.auth;

import androidx.annotation.Nullable;

/**
 * Seam between the data layer and whoever reacts to an expired session. A
 * repository that sees an HTTP 401 calls {@link #invalidate()}; the UI
 * registers the listener at app start (wipe session, toast, go to login), so
 * the data layer never depends on screens.
 */
public final class SessionInvalidator {

  /** Reacts to session lifecycle events raised by the data layer. */
  public interface Listener {
    /** Any request answered 401: the stored token is no longer valid. */
    void onSessionInvalidated();

    /** A login succeeded: a later expiry must be handled again. */
    void onSessionStarted();
  }

  @Nullable private static volatile Listener listener;

  private SessionInvalidator() {}

  public static void setListener(@Nullable Listener value) {
    listener = value;
  }

  public static void invalidate() {
    Listener local = listener;
    if (local != null) {
      local.onSessionInvalidated();
    }
  }

  public static void sessionStarted() {
    Listener local = listener;
    if (local != null) {
      local.onSessionStarted();
    }
  }
}
