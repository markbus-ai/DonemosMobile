package com.donemosmobile.ui.auth;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.donemosmobile.data.auth.SessionInvalidator;
import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.SessionStore;
import com.donemosmobile.data.network.ApiClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single place that reacts to an HTTP 401 from any repository: wipes the
 * token, the cached user and the in-memory data cache, then sends the user to
 * login with a clear message. Several requests failing with 401 at once only
 * trigger one redirect. Registered once at app start through
 * {@link #register(Context)}.
 */
public final class SessionExpired implements SessionInvalidator.Listener {

  public static final String MESSAGE = "Tu sesión venció, ingresá de nuevo.";

  private static final AtomicBoolean REDIRECTING = new AtomicBoolean(false);

  private final Context app;

  private SessionExpired(Context app) {
    this.app = app;
  }

  /** Makes this the data layer's reaction to an expired session. */
  public static void register(Context context) {
    SessionInvalidator.setListener(new SessionExpired(context.getApplicationContext()));
  }

  @Override
  public void onSessionInvalidated() {
    ApiClient.get(app).tokenStore().clear();
    SessionStore.getInstance(app).clear();
    DataCache.getInstance().clear();
    if (!REDIRECTING.compareAndSet(false, true)) {
      return;
    }
    new Handler(Looper.getMainLooper()).post(() -> {
      Toast.makeText(app, MESSAGE, Toast.LENGTH_LONG).show();
      SessionActions.goToLogin(app);
    });
  }

  /** After a successful login a later expiry redirects again. */
  @Override
  public void onSessionStarted() {
    REDIRECTING.set(false);
    SessionActions.resetLogout();
  }
}
