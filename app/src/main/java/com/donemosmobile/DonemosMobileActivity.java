package com.donemosmobile;

import android.content.Intent;
import android.os.Bundle;

import com.donemosmobile.data.auth.AuthRepository;
import com.donemosmobile.data.cache.Prefetcher;
import com.donemosmobile.data.network.dto.UserDto;
import com.donemosmobile.ui.auth.LoginActivity;
import com.donemosmobile.ui.home.Home;

public class DonemosMobileActivity extends AppActivity {

  private AuthRepository repository;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    repository = new AuthRepository(this);

    // Sin token no hay nada que mostrar: directo al login.
    if (!repository.hasSession()) {
      goToLogin();
      return;
    }

    // Con sesion, arranca el precalentado en 2do plano antes de mostrar la Home:
    // asi las primeras pantallas levantan datos del cache al instante.
    Prefetcher.warmUp(this);

    setContentView(new Home(this));
    validateSession();
  }

  /**
   * Confirma el token contra el backend. Si venció (401) se limpia la sesión y
   * se vuelve al login; si fue un error de red se mantiene la sesión cacheada.
   */
  private void validateSession() {
    repository.me(new AuthRepository.Callback<UserDto>() {
      @Override
      public void onSuccess(UserDto data) {
        // Sesión válida, la Home ya está en pantalla.
      }

      @Override
      public void onError(AuthRepository.ErrorType type, String message) {
        if (type == AuthRepository.ErrorType.UNAUTHORIZED) {
          goToLogin();
        }
      }
    });
  }

  private void goToLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
