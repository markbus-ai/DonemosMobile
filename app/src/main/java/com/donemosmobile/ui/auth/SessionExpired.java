package com.donemosmobile.ui.auth;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.SessionStore;
import com.donemosmobile.data.network.ApiClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single place that reacts to an HTTP 401 from any repository: wipes the
 * token, the cached user and the in-memory data cache, then sends the user to
 * login with a clear message. Several requests failing with 401 at once only
 * trigger one redirect.
 */
public final class SessionExpired {

  public static final String MESSAGE = "Tu sesión venció, ingresá de nuevo.";

  private static final AtomicBoolean REDIRECTING = new AtomicBoolean(false);

  private SessionExpired() {}

  public static void handle(Context context) {
    final Context app = context.getApplicationContext();
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

  /** Called after a successful login so a later expiry redirects again. */
  public static void reset() {
    REDIRECTING.set(false);
    SessionActions.resetLogout();
  }
}
