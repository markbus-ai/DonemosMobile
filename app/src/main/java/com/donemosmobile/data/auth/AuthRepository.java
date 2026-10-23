package com.donemosmobile.data.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.SessionStore;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.AuthApi;
import com.donemosmobile.data.network.dto.LoginRequest;
import com.donemosmobile.data.network.dto.LoginResponse;
import com.donemosmobile.data.network.dto.UserDto;
import com.donemosmobile.ui.auth.SessionExpired;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Punto de entrada a auth. Corre la red con {@code enqueue} de Retrofit (fuera
 * del hilo principal) y entrega el resultado por callback en el hilo principal.
 */
public class AuthRepository {

  private final AuthApi api;
  private final TokenStore tokenStore;
  private final SessionStore sessionStore;

  public AuthRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.auth();
    this.tokenStore = client.tokenStore();
    this.sessionStore = SessionStore.getInstance(context);
  }

  /** Autentica y persiste el token. Devuelve el usuario en {@code onSuccess}. */
  public void login(String username, String password, final ApiCallback<UserDto> callback) {
    api.login(new LoginRequest(username, password))
        .enqueue(new retrofit2.Callback<LoginResponse>() {
          @Override
          public void onResponse(@NonNull Call<LoginResponse> call,
              @NonNull Response<LoginResponse> response) {
            LoginResponse body = response.body();
            if (response.isSuccessful() && body != null
                && body.token != null && !body.token.isEmpty()) {
              // Never leak cached data from a previous session into this one.
              DataCache.getInstance().clear();
              tokenStore.saveToken(body.token);
              sessionStore.save(body.user);
              SessionExpired.reset();
              callback.onSuccess(body.user);
              return;
            }
            if (response.code() == 401) {
              callback.onError(ApiError.Type.INVALID_CREDENTIALS, extractMessage(response));
              return;
            }
            if (response.code() == 429) {
              callback.onError(ApiError.Type.TOO_MANY_ATTEMPTS, extractMessage(response));
              return;
            }
            callback.onError(mapStatus(response.code()), extractMessage(response));
          }

          @Override
          public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
            callback.onError(ApiError.Type.NETWORK, t.getMessage());
          }
        });
  }

  /**
   * Valida la sesión actual contra el backend. Un 401 limpia el token guardado
   * y responde {@link ApiError.Type#UNAUTHORIZED}.
   */
  public void me(final ApiCallback<UserDto> callback) {
    api.me().enqueue(new retrofit2.Callback<UserDto>() {
      @Override
      public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
        if (response.isSuccessful()) {
          // Refresca los datos de presentación del usuario en cada validación.
          sessionStore.save(response.body());
          callback.onSuccess(response.body());
          return;
        }
        if (response.code() == 401) {
          clearSession();
          callback.onError(ApiError.Type.UNAUTHORIZED, extractMessage(response));
          return;
        }
        callback.onError(mapStatus(response.code()), extractMessage(response));
      }

      @Override
      public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /**
   * Cierra la sesión. La limpieza local ocurre siempre, aunque el server no
   * responda; el callback se dispara igual para no trabar la UI.
   */
  public void logout(final ApiCallback<Void> callback) {
    api.logout().enqueue(new retrofit2.Callback<Void>() {
      @Override
      public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
        clearSession();
        callback.onSuccess(null);
      }

      @Override
      public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
        clearSession();
        callback.onSuccess(null);
      }
    });
  }

  public void clearSession() {
    tokenStore.clear();
    sessionStore.clear();
    // El cache es de la sesion: al cerrarla no dejamos datos del usuario anterior.
    DataCache.getInstance().clear();
  }

  public boolean hasSession() {
    return tokenStore.hasToken();
  }

  /**
   * Auth keeps its own coarse mapping: a 401 on login means bad credentials,
   * not an expired session, and any other 4xx shows the backend copy.
   */
  private ApiError.Type mapStatus(int code) {
    return code >= 500 ? ApiError.Type.SERVER : ApiError.Type.UNKNOWN;
  }

  @Nullable
  private String extractMessage(Response<?> response) {
    return ApiError.read(response).message;
  }
}
