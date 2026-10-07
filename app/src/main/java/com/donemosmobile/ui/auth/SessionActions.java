package com.donemosmobile.ui.auth;

import android.content.Context;
import android.content.Intent;

import com.donemosmobile.data.auth.AuthRepository;

/**
 * Acciones de sesión compartidas. Cerrar sesión limpia lo local aunque el
 * backend no responda y vuelve al login limpiando el back stack, para que no
 * se pueda volver atrás con la sesión cerrada.
 */
public final class SessionActions {

  private SessionActions() {}

  public static void logout(Context context) {
    new AuthRepository(context).logout(new AuthRepository.Callback<Void>() {
      @Override
      public void onSuccess(Void data) {
        goToLogin(context);
      }

      @Override
      public void onError(AuthRepository.ErrorType type, String message) {
        // logout() limpia la sesión local siempre: igual vamos al login.
        goToLogin(context);
      }
    });
  }

  public static void goToLogin(Context context) {
    Intent intent = new Intent(context, LoginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    context.startActivity(intent);
  }
}
