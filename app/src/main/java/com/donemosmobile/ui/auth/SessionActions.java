package com.donemosmobile.ui.auth;

import android.content.Context;
import android.content.Intent;

import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.auth.AuthRepository;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Acciones de sesión compartidas. Cerrar sesión limpia lo local aunque el
 * backend no responda y vuelve al login limpiando el back stack, para que no
 * se pueda volver atrás con la sesión cerrada.
 */
public final class SessionActions {

  private SessionActions() {}

  /** Blocks repeated taps on "Salir" while the logout request is in flight. */
  private static final AtomicBoolean LOGGING_OUT = new AtomicBoolean(false);

  public static void logout(Context context) {
    if (!LOGGING_OUT.compareAndSet(false, true)) {
      return;
    }
    new AuthRepository(context).logout(new ApiCallback<Void>() {
      @Override
      public void onSuccess(Void data) {
        goToLogin(context);
      }

      @Override
      public void onError(ApiError.Type type, String message) {
        // logout() limpia la sesión local siempre: igual vamos al login.
        goToLogin(context);
      }
    });
  }

  /** Re-arms logout after a new login. */
  static void resetLogout() {
    LOGGING_OUT.set(false);
  }

  public static void goToLogin(Context context) {
    Intent intent = new Intent(context, LoginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    context.startActivity(intent);
  }
}
